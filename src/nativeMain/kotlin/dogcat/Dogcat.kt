package dogcat

import Config
import dogcat.LogFilter.Substring
import dogcat.LogcatState.WaitingInput
import dogcat.StartupAs.*
import flow.bufferedTransform
import flow.takeUntil
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.*

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalForeignApi::class)
class Dogcat(
    private val logLinesSource: LogLinesSource,
    private val s: InternalState = InternalState(),
    private val dispatcherCpu: CoroutineDispatcher = Dispatchers.Default,
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO,
    private val lineParser: LogLineParser = LogcatBriefParser()

)  {
    val handler = CoroutineExceptionHandler { _, t -> Logger.d("999999 ${t.message}\r") }
    private val scope = CoroutineScope(dispatcherCpu + handler + Job()) // +Job +SupervisorJob +handler

    private val privateState = MutableStateFlow<LogcatState>(WaitingInput)
    val state = privateState.asStateFlow()

    private val stopSubject = MutableSharedFlow<Unit>()
    private val filterLine = MutableStateFlow<String>("")

    private fun filterLines(): Flow<IndexedValue<LogLine>> {
        val sharedLines = logLinesSource // deal with malformed UTF-8 'expected one more byte'
            .lines()
            .retry(3) { e ->
                val shallRetry = e is RuntimeException
                if (shallRetry) delay(100)
                Logger.d("retrying... $shallRetry\r")
                shallRetry
            }
            //.takeUntil(stopSubject)
            .onCompletion { cause -> if (cause == null) emit("INPUT HAS EXITED") else Logger.d("EXIT COMPLETE $cause\r") }
            .onStart { Logger.d("start logcat lines\r") }
            .shareIn(
                scope,
                SharingStarted.Lazily,
                Config.LogLinesBufferCount,
            )
            .onSubscription { Logger.d("subscr to shared lines\r") }
            .onCompletion { Logger.d("shared compl!\r") }


        return filterLine
            .flatMapLatest { filter ->
                sharedLines.filter { it.contains(filter) }
            }
            .map {
                lineParser.parse(it)
            }
            .bufferedTransform(
                { buffer, item ->
                    val s = buffer.size
                    when {
                        item is Original -> true
                        s > 0 -> {
                            val previous = buffer[0]

                            val r = when {
                                (item is Parsed) && (previous is Parsed) && (item.tag.contains(previous.tag)) -> false
                                else -> true
                            }
                            r
                        }
                        else -> false
                    }
                },
                { buffer, item ->
                    if (buffer.isEmpty()) {
                        item
                    } else {
                        if (item is Parsed) {
                            Parsed(item.level, "".padStart(40), item.owner, item.message)
                        } else {
                            item
                        }
                    }
                }
            )
            .withIndex()
            //.flowOn()
            .onCompletion { Logger.d("outer compl\r") }
    }

    suspend operator fun invoke(cmd: Command) {
        when (cmd) {
            is StartupAs -> startup(cmd)
            ClearLogs -> clearLogs()

            is FilterBy -> {
                s.upsertFilter(cmd.filter, false)
                if (cmd.filter is Substring) {
                    filterLine.emit(cmd.filter.substring)
                }
                startupAll()
            }

            is ClearFilter -> {
            }

            StopEverything -> {
                privateState.emit(LogcatState.Terminated)
                stopSubject.emit(Unit)

                //dumpCoroutines()
                scope.ensureActive()
                scope.cancel()
            }
        }
    }

    private suspend fun clearLogs() {
        Logger.d("to clear logs\r")

        logLinesSource.clear()
        stopSubject.emit(Unit)

        val ci = LogcatState.InputCleared

        Logger.d("Input cleared -+")

        privateState.emit(ci)

        startupAll()
    }

    private suspend fun startup(command: StartupAs) {
        when (command) {
            is WithForegroundApp -> {
                val packageName = ForegroundProcess.parsePackageName()
                val userId = DumpsysPackage().parseUserIdFor(packageName)

                s.upsertFilter(LogFilter.ByPackage(packageName, userId), true)
                Logger.d("Startup with foreground app, resolved to package '$packageName' and user ID '$userId'")
            }

            is WithPackage -> {
                val packageName = command.packageName
                val userId = DumpsysPackage().parseUserIdFor(packageName)

                s.upsertFilter(LogFilter.ByPackage(packageName, userId), true)
                Logger.d("Startup with foreground app, resolved to package '$packageName' and user ID '$userId'")
            }

            is All -> {
                Logger.d("Startup with no package filters")
            }
        }

        startupAll()
    }

    private suspend fun startupAll() {
        val filterLines = filterLines()

        val ci = LogcatState.CapturingInput(
            filterLines
                .onCompletion { Logger.d("COMPLETION $it\r") } //called when scope is cancelled as well
                .takeUntil(stopSubject),

            s.appliedFilters
        )
        privateState.emit(ci)
    }
}
