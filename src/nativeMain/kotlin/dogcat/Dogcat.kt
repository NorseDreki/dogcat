package dogcat

import Config
import dogcat.LogFilter.Substring
import platform.LogLineParser
import platform.LogcatBriefParser
import dogcat.LogcatState.WaitingInput
import flow.bufferedTransform
import flow.takeUntil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.ForegroundProcess
import platform.RunningProcesses
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
class Dogcat(
    private val logSource: LogSource,
    private val s: InternalState = InternalState(),
    private val dispatcherCpu: CoroutineDispatcher = Dispatchers.Default,
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO,
    private val lineParser: LogLineParser = LogcatBriefParser()

)  {
    val handler = CoroutineExceptionHandler { _, t -> println("999999 ${t.message}\r") }
    private val scope = CoroutineScope(dispatcherCpu + handler) // +Job +SupervisorJob +handler

    private val privateState = MutableStateFlow<LogcatState>(WaitingInput)
    val state = privateState.asStateFlow()

    private val stopSubject = MutableSharedFlow<Unit>()
    private val filterLine = MutableStateFlow<String>("")

    private fun filterLines(): Flow<IndexedValue<LogLine>> {
        val sharedLines = logSource // deal with malformed UTF-8 'expected one more byte'
            .lines()
            .retry(3) { e ->
                val shallRetry = e is RuntimeException
                if (shallRetry) delay(100)
                println("retrying... $shallRetry\r")
                shallRetry
            }
            .takeUntil(stopSubject)
            .onCompletion { cause -> if (cause == null) emit("INPUT HAS EXITED") else println("EXIT COMPLETE $cause\r") }
            .flowOn(dispatcherIo)
            .onStart { println("start logcat lines\r") }
            .shareIn(
                scope,
                SharingStarted.Lazily,
                Config.LogLinesBufferCount,
            )
            .onSubscription { println("subscr to shared lines\r") }
            .onCompletion { println("shared compl!\r") }


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
            .onCompletion { println("outer compl\r") }
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
        println("to clear logs\r")
        stopSubject.emit(Unit)

        logSource.clear()
        val ci = LogcatState.InputCleared

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
                .onCompletion { println("COMPLETION $it\r") } //called when scope is cancelled as well
                .takeUntil(stopSubject),

            s.appliedFilters
        )
        privateState.emit(ci)
    }
}
