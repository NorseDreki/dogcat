import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class Logcat {

    //viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)

    private val privateState = MutableStateFlow<LogcatState>(LogcatState.Waiting)

    val scope = CoroutineScope(Dispatchers.Default)

    private val lines = MutableSharedFlow<String>()

    val startSubject = MutableSharedFlow<Unit>()

    val filterLine = MutableStateFlow<String>("")

    val ss = startSubject
        .flatMapLatest {
            println("to start logcat command")
            startLogcat().flowOn(Dispatchers.IO)
        }
        .shareIn(
            scope,
            SharingStarted.Eagerly,
            50000,
        )

    val sss = filterLine
        .flatMapLatest { filter ->
            ss.filter { it.contains(filter) }
        }

    private fun startLogcat(): Flow<String> {
        println("11111 start LOGCAT")

        return flow {
            val child = Command("adb")
                .args("logcat", "-v", "brief")
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader: com.kgit2.io.Reader? = child.getChildStdout()

            while (true) {
                val line2 = stdoutReader!!.readLine() ?: break
                emit(line2)
                //if (isActive)
                yield()
            }
        }
    }

    private fun colorize(): String {
        TODO("Not yet implemented")
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
            is Filter.ByLogLevel -> TODO()
            is Filter.ByString -> filterWith(cmd)
            is Filter.ByTime -> TODO()
            is Filter.Package -> TODO()
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
        scope.launch {
            yield()
            startSubject.emit(Unit)
        }
    }
}
