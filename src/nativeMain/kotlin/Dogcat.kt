import LogcatState.WaitingInput
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class Dogcat(
    private val logSource: LogSource,
    dispatcherCpu: CoroutineDispatcher = Dispatchers.Default,
    dispatcherIo: CoroutineDispatcher = Dispatchers.IO,
    private val lineParser: LogLineParser = LogcatBriefParser()
)  {
    val handler = CoroutineExceptionHandler { _, t -> println("999999 ${t.message}\r") }
    private val scope = CoroutineScope(dispatcherCpu + handler) // +Job +SupervisorJob +handler

    private val privateState = MutableStateFlow<LogcatState>(WaitingInput)
    val state = privateState.asStateFlow()

    private val stopSubject = MutableSharedFlow<Unit>()
    private val filterLine = MutableStateFlow<String>("")

    private val logLevels = mutableSetOf<String>("V", "D", "I", "W", "E") //+.WTF()?

    //re-create each time clearing occurs
    private val sharedLines = logSource // deal with malformed UTF-8 'expected one more byte'
        .lines()
        //.onEach { println("----- >> each $it\r") }
        .retry(3) { e ->
            println("retrying...\r")
            val shallRetry = e is RuntimeException
            if (shallRetry) delay(100)
            println("retrying... $shallRetry\r")
            shallRetry
        }
        .onCompletion { cause -> if (cause == null) emit("INPUT HAS EXITED") else println("EXIT COMPLETE $cause\r")}
        .flowOn(dispatcherIo)
        .onEmpty { println("empty\r") }
        .onStart { println("start logcat lines\r") }
        .shareIn(
            scope,
            SharingStarted.Lazily,
            Config.LogLinesBufferCount,
        )
        .onSubscription { println("subscr to shared lines\r") }
        .onCompletion { println("shared compl!\r") }

    private fun filterLines() = filterLine
        .flatMapLatest { filter ->
            sharedLines
                .filter { it.contains(filter) }
        }
        .map { lineParser.parse(it) }
        .filter {
            if (it is Parsed) {
                logLevels.contains(it.level)
            } else {
                true
            }
        }
        //.flowOn()
        .onCompletion { println("outer compl\r") }

    suspend operator fun invoke(cmd: LogcatCommands) {
        when (cmd) {
            StartupAs.All -> startupAll()
            ClearLogs -> clearLogs()
            is FilterWith -> filterWith(cmd.filter)
            is ClearFilter -> clearFilter()
            is Filter.Exclude -> TODO()
            is Filter.ToggleLogLevel -> filterByLogLevel(cmd)
            is Filter.ByString -> filterWith(cmd)
            is Filter.ByTime -> TODO()
            is Filter.Package -> TODO()
            StopEverything -> {
                privateState.emit(LogcatState.Terminated)
                stopSubject.emit(Unit)

                scope.ensureActive()
                scope.cancel()
                println("cancelled scope ${scope.coroutineContext.isActive}\r")
            }
        }
    }

    private fun filterByLogLevel(cmd: Filter.ToggleLogLevel) {
        // concurrent access protection?
        if (logLevels.contains(cmd.level)) {
            logLevels.remove(cmd.level)
        } else {
            logLevels.add(cmd.level)
        }
    }

    private fun clearFilter() {
    }

    private suspend fun filterWith(filter: Filter) {
        if (filter is Filter.ByString) {
            filterLine.emit(filter.substring)
            //println("next filter ${filter.substring}")
        }
    }

    private suspend fun clearLogs() {
        println("to clear logs\r")

        stopSubject.emit(Unit)
        println("called stop subject..\r")

        //sharedLines.rese

        logSource.clear()
        val ci = LogcatState.InputCleared

        privateState.emit(ci)

        startupAll()
    }

    private suspend fun startupAll() {
        val ci = LogcatState.CapturingInput(
            filterLines()
                .onCompletion { println("COMPLETION $it\r") } //called when scope is cancelled as well
                .takeUntil(stopSubject)
        )

        privateState.emit(ci)
    }
}
