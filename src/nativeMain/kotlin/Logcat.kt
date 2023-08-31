import LogcatState.WaitingInput
import com.kgit2.process.Command
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
    val state = privateState.asSharedFlow()

    private val startSubject = MutableSharedFlow<Unit>(1)
    private val filterLine = MutableStateFlow<String>("")
    private val logLevels = mutableSetOf<String>("V", "D", "I", "W", "E") //+.WTF()?

    private val ss = startSubject
        //beware of implicit distinctuntilchanged
        .flatMapLatest {
            println("to start logcat command")
            logSource
                .lines()
                //.catch { cause -> emit("Emit on error") } // deal with malformed UTF-8 'expected one more byte'
                .retry(3) { e ->
                    println("retrying...")
                    val shallRetry = e is RuntimeException
                    if (shallRetry) delay(100)
                    println("retrying... $shallRetry")
                    shallRetry
                }
                .onCompletion { cause -> if (cause == null) emit("INPUT HAS EXITED") }
                .flowOn(dispatcherIo)
        }
        .shareIn(
            scope,
            SharingStarted.Eagerly,
            50000,
        )

    private val sss = filterLine
        .flatMapLatest { filter ->
            ss
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
        //.takeUnless {  }

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
            StopEverything -> scope.cancel()
        }
    }

    private fun filterByLogLevel(cmd: Filter.ToggleLogLevel) {
        if (logLevels.contains(cmd.level)) {
            logLevels.remove(cmd.level)
        } else {
            logLevels.add(cmd.level)
        }
    }

    private fun clearFilter() {
    }

    private fun filterWith(filter: Filter) {
        println("filter lkjshdkjlfhskjdlhfkljdsh 1213123")
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

            val childCode = Command("adb")
                .args("logcat", "-c")
                .spawn()
                .start()
                //.wait()

            println("exit code: ")

            val ci = LogcatState.InputCleared

            privateState.emit(ci)

            startupAll()
        }
    }

    private fun startupAll() {
        println("jhkjhkjh")
        scope.launch {
            println("jhkjhkjh  11111")
            //yield()
            startSubject.emit(Unit)

            val ci = LogcatState.CapturingInput(
                sss
            )

            println("prepare to emit capturing")
            privateState.emit(ci)

            println("emitted capturing")
            //yield()
        }
    }
}
