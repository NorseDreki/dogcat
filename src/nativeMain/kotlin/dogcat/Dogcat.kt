package dogcat

import Config
import dogcat.Command.*
import dogcat.Command.StartupAs.*
import dogcat.LogFilter.ByPackage
import dogcat.LogFilter.Substring
import dogcat.LogcatState.WaitingInput
import flow.bufferedTransform
import flow.takeUntil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.*

@OptIn(ExperimentalCoroutinesApi::class)
class Dogcat(
    private val logLinesSource: LogLinesSource,
    private val s: InternalQuery = InternalQuery(),
    private val dispatcherCpu: CoroutineDispatcher = Dispatchers.Default,
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO,
    private val lineParser: LogLineParser = LogcatBriefParser()

)  {
    val handler = CoroutineExceptionHandler { _, t -> Logger.d("CATCH! ${t.message}\r") }
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


        /*s.appliedFilters
            .flatMapConcat { it.values.asFlow() }
            .map { it.first }
            .filterIsInstance<Substring>()*/

       // return filterLine

        return s.appliedFilters
            .flatMapConcat { it.values.asFlow() }
            .map { it.first }
            .filterIsInstance<Substring>()
            .flatMapLatest { filter ->
                sharedLines.filter { it.contains(filter.substring) }
            }
            .map {
                lineParser.parse(it)
            }
            .bufferedTransform(
                { buffer, item ->
                    when {
                        buffer.isNotEmpty() -> {
                            val previous = buffer[0]
                            when {
                                item.tag.contains(previous.tag) -> false
                                else -> true
                            }
                        }
                        else -> false
                    }
                },
                { buffer, item ->
                    if (buffer.isEmpty()) {
                        item
                    } else {
                        LogLine(item.level, "", item.owner, item.message)
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
                /*if (cmd.filter is Substring) {
                    filterLine.emit(cmd.filter.substring)
                }*/

                val ci = LogcatState.InputCleared

                Logger.d("Input cleared -+")

                privateState.emit(ci)


                doStartup()
            }

            is ResetFilter -> {
                when (cmd.filter) {
                    Substring::class -> s.upsertFilter(Substring(""))
                    else -> s.removeFilter(cmd.filter)
                }
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

        doStartup()
    }

    private suspend fun startup(command: StartupAs) {
        when (command) {
            is WithForegroundApp -> {
                val packageName = ForegroundProcess.parsePackageName()
                val userId = DumpsysPackage().parseUserIdFor(packageName)

                s.upsertFilter(ByPackage(packageName, userId), true)
                Logger.d("Startup with foreground app, resolved to package '$packageName' and user ID '$userId'")
            }

            is WithPackage -> {
                val packageName = command.packageName
                val userId = DumpsysPackage().parseUserIdFor(packageName)

                s.upsertFilter(ByPackage(packageName, userId), true)
                Logger.d("Startup package name '$packageName', resolved user ID to '$userId'")
            }

            is All -> {
                Logger.d("Startup with no package filters")
            }
        }

        doStartup()
    }

    private suspend fun doStartup() {
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
