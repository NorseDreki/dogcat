import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class Logcat(
    val logSource: LogSource
)  {

    //viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)

    private val privateState = MutableStateFlow<LogcatState>(LogcatState.Waiting)

    val scope = CoroutineScope(Dispatchers.Default)

    private val lines = MutableSharedFlow<String>()

    val startSubject = MutableSharedFlow<Unit>()

    val filterLine = MutableStateFlow<String>("")

    val ss = startSubject
        .flatMapLatest {
            println("to start logcat command")
            logSource.lines()//.flowOn(Dispatchers.IO)
        }
        .shareIn(
            scope,
            SharingStarted.Eagerly,
            50000,
        )

    val logLevels = mutableSetOf<String>("V", "D", "I", "W", "E")

    val sss = filterLine
        .flatMapLatest { filter ->
            ss
                .filter { it.contains(filter) }
        }
        .map { colorize(it) }
        .filter { logLevels.contains(it.level) }

    private fun colorize(line: String): LogLine {
        val r2 = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

        val greenColor = "\u001b[31;1;4m"
        val reset = "\u001b[0m" // to reset color to the default
        val name = greenColor + "Alex" + reset // Add green only to Alex

        val m = r2.matchEntire(line)
        return if (m != null) {
            //   println("11111 $line")
            val (level, tag, owner, message) = m.destructured

            //println(line)

            LogLine(level, tag, owner, message)

            //"$name $level:$greenColor$tag$reset [$owner] /$message/"

            //name

        } else {
            //"ERROR"

            LogLine("", "", "", "")
        }
    }

    fun addLine(line: String) {
        scope.launch {
            lines.emit(line)
        }
    }

    fun processCommand(cmd: LogcatCommands) {
        println("22222222111111kjlhdf process")
        when (cmd) {

            StartupAs.All -> startupAll()

            ClearLogs -> clearLogs()

            is FilterWith -> filterWith(cmd.filter)

            is ClearFilter -> clearFilter()
            is Exclude -> TODO()
            is Filter.ByLogLevel -> filterByLogLevel(cmd)
            is Filter.ByString -> filterWith(cmd)
            is Filter.ByTime -> TODO()
            is Filter.Package -> TODO()
        }
    }

    private fun filterByLogLevel(cmd: Filter.ByLogLevel) {
        if (logLevels.contains(cmd.level)) {
            logLevels.remove(cmd.level)
        } else {
            logLevels.add(cmd.level)
        }
    }

    private fun clearFilter() {
        val child = Command("adb")
            .args("logcat", "-с")
            .stdout(Stdio.Pipe)
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
        TODO("Not yet implemented")
    }

    private fun startupAll() {

        println("jhkjhkjh")
        scope.launch {
            println("jhkjhkjh  11111")
            yield()
            startSubject.emit(Unit)
        }
    }
}
