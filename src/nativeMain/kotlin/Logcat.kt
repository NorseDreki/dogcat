import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class Logcat {

    private val privateState = MutableStateFlow<LogcatState>(LogcatState.Waiting)

    val scope = CoroutineScope(Dispatchers.Default)

    private val lines = MutableSharedFlow<String>()
    //.filter {  }

    val l = lines
        //.combine(filter) {
        //
        //}
        .map { colorize() }

    val startSubject = MutableSharedFlow<Unit>()

    val filterLine = MutableStateFlow<String>("a")

    val ss = startSubject
        .map { println("jlkhjklhkhjh -- subject") }
        .flatMapLatest {
            println("111ljkhlkwjhfkljh flatmap")
            startLogcat()
                /*.shareIn(
                    scope,
                    SharingStarted.Lazily,
                    50000,
                )*/
        }
        .combine(filterLine) { left, right ->
            //println("1111111 filter [$right] item $left")
            if (left.contains(right)) {
                left
            } else {
                null
            }
        }
        .filterNotNull()
        //.map { colorize() }

    val ff = ss
    //.filter { //level W, E }


    private fun startLogcat(): Flow<String> {
        println("11111 start LOGCAT")
        return flow {

            println("111111111 jjjjjj")
            //withContext(Dispatchers.IO) {
                val child = Command("adb")
                    .args("logcat", "-v", "brief")
                    .stdout(Stdio.Pipe)
                    .spawn()

                val stdoutReader: com.kgit2.io.Reader? = child.getChildStdout()

                while (true) {
                    val line2 = stdoutReader!!.readLine() ?: break
                    //delay(1.microseconds)
                    emit(line2)

                    //if (isActive)

                    yield()
                }
            //}
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
        //println("kjlhdf")
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


        /*j?.cancelAndJoin()

        j = lg
            .filter { it.contains(bytePtr.toKString()) }
            //.take(10)
            //.take(10)
            .withIndex()
            .onEach {
                //if (it.value != null) {
                //println("${it.index} ${it.value} \r\n")
                waddstr(fp, "${it.index} ${it.value}\n")

                prefresh(fp, it.index, 0, 3, 0, sy - 1, sx)

                a = it.index

                yield()
                //}
                //
            }
            .launchIn(this)*/
    }

    private fun clearLogs() {
        TODO("Not yet implemented")
    }

    private fun startupAll() {
        println("222 2 2 2  2  dkjfhalkjdhf")
        scope.launch {
            yield()
            println("132lkuhgfwkjehf -- start")
            startSubject.emit(Unit)
            println("132lkuhgfwkjehf -- emitted")
        }
    }
}
