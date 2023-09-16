package dogcat

import Config
import dogcat.LogFilter.Substring
import dogcat.LogcatState.WaitingInput
import flow.bufferedTransform
import flow.takeUntil
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.*
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalForeignApi::class)
class Dogcat(
    private val logSource: LogSource,
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
        val sharedLines = logSource // deal with malformed UTF-8 'expected one more byte'
            .lines()
            /*.retry(3) { e ->
                val shallRetry = e is RuntimeException
                if (shallRetry) delay(100)
                Logger.d("retrying... $shallRetry\r")
                shallRetry
            }*/
            .takeUntil(stopSubject)
            .onCompletion { cause -> if (cause == null) emit("INPUT HAS EXITED") else Logger.d("EXIT COMPLETE $cause\r") }
            //.flowOn(dispatcherIo)
            .onStart { Logger.d("start logcat lines\r") }
            .shareIn(
                scope,
                //SharingStarted.WhileSubscribed(0),
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

    suspend operator fun invoke(cmd: LogcatCommands) {
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

        logSource.clear()

        stopSubject.emit(Unit)
        //scope.cancel()

        val ci = LogcatState.InputCleared

        Logger.d("Input cleared -+")

        privateState.emit(ci)

        startupAll()
    }

    private suspend fun startup(cmd: StartupAs) {
        var p = ""//""com.norsedreki.multiplatform.identity.android"
        val pid = if (cmd is StartupAs.WithForegroundApp) {
            ForegroundProcess.parsePs()
        } else if (cmd is StartupAs.WithPackage) {
            p = cmd.packageName
            RunningProcesses().parsePs(cmd.packageName)
        } else {
            ""
        }

        s.upsertFilter(LogFilter.ByPackage(p, "10151"), true)

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
