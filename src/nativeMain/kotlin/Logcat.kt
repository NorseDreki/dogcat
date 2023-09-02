import LogcatState.WaitingInput
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class Logcat(
    private val logSource: LogSource,
    dispatcherCpu: CoroutineDispatcher = Dispatchers.Default,
    dispatcherIo: CoroutineDispatcher = Dispatchers.IO,
)  {
    val handler = CoroutineExceptionHandler { _, t -> println("999999 ${t.message}") }
    private val scope = CoroutineScope(dispatcherCpu + handler) // +Job +SupervisorJob +handler

    private val privateState = MutableStateFlow<LogcatState>(WaitingInput)
    val state = privateState.asStateFlow()

    private val stopSubject = MutableSharedFlow<Unit>()
    private val filterLine = MutableStateFlow<String>("")

    private val logLevels = mutableSetOf<String>("V", "D", "I", "W", "E") //+.WTF()?

    private val sharedLines = logSource // deal with malformed UTF-8 'expected one more byte'
        .lines()
        .onEach { println("each $it") }
        .retry(3) { e ->
            println("retrying...")
            val shallRetry = e is RuntimeException
            if (shallRetry) delay(100)
            println("retrying... $shallRetry")
            shallRetry
        }
        .onCompletion { cause -> if (cause == null) emit("INPUT HAS EXITED") else println("EXIT COMPLETE $cause")}
        .flowOn(dispatcherIo)
        .onEmpty { println("empty") }
        .onStart { println("start") }
        .shareIn(
            scope,
            SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
            Config.LogLinesBufferCount,
        )
                .onSubscription {
                    println("subscr")
                }
                .onCompletion { println("shared compl") }


    private val filteredLines = filterLine
        .onCompletion { println("fl compl") }
        .flatMapLatest { filter ->
            sharedLines
                .onEach { println("each $it") }
                .filter { it.contains(filter) }
        }
        .map { parse(it) }
        .filter {
            if (it is Parsed) {
                logLevels.contains(it.level)
            } else {
                true
            }
        }
        .onCompletion { println("outer compl") }

    private fun parse(line: String): LogLine {
        val r2 = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

        val m = r2.matchEntire(line)
        return if (m != null) {
            val (level, tag, owner, message) = m.destructured
            Parsed(level, tag, owner, message)
        } else {
            Original(line)
        }
    }

    operator fun invoke(cmd: LogcatCommands) {
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
                scope.launch {
                    privateState.emit(LogcatState.Terminated)

                    stopSubject.emit(Unit)
                }
                scope.ensureActive()
                scope.cancel()
                println("cancelled scope ${scope.coroutineContext.isActive}")
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

    private fun filterWith(filter: Filter) {
        scope.launch {
            if (filter is Filter.ByString) {
                filterLine.emit(filter.substring)
                println("next filter ${filter.substring}")
            }
        }
    }

    private fun clearLogs() {
        println("to clear logs")
        scope.launch {
            println("clearing..")

            stopSubject.emit(Unit)
            println("called stop subject..")

            logSource.clear()
            val ci = LogcatState.InputCleared

            privateState.emit(ci)

            startupAll()
        }
    }

    private fun startupAll() {
        scope.launch {
            println("jhkjhkjh  11111")
            val ci = LogcatState.CapturingInput(
                filteredLines
                    .onCompletion { println("COMPLETION $it") } //called when scope is cancelled as well
                    .takeUntil(stopSubject)
            )

            println("prepare to emit capturing")
            privateState.emit(ci)

            println("emitted capturing")
        }
    }
}
