import LogcatState.WaitingInput
import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class Dogcat(
    private val logSource: LogSource,
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

    private val logLevels = mutableSetOf<String>("V", "D", "I", "W", "E") //+.WTF()? *.F

    val excludedTags = hashSetOf(
        "droid.apps.mai",
        "OpenGLRenderer",
        "CpuPowerCalculator",
        "EGL_emulation",
        "libEGL",
        "Choreographer",
        "HeterodyneSyncer"
    )

    data class History<T>(val previous: T?, val current: T)

    // emits null, History(null,1), History(1,2)...
    fun <T> Flow<T>.runningHistory(): Flow<History<T>?> =
        runningFold(
            initial = null as (History<T>?),
            operation = { accumulator, new -> History(accumulator?.current, new) }
        )


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
            //.map { lineParser.parse(it) }
            .map {
                /*val pe = parseProcessDeath(it)
                if (pe != null) {
                    println("11111 $pe\r")
                    Parsed("V", pe.first, "", pe.second)
                } else {
                    null
                }*/

                val ps = parseProcessStart(it)
                if (ps != null) {
                    if (ps.first.contains(p)) {
                        pids.add(ps.second)
                    }

                    //println("11111 $ps\r")
                    Parsed("E", ps.first, ps.second, ps.second)
                } else {
                    val pe = parseProcessDeath(it)
                    if (pe != null) {
                        if (pe.first.contains(p)) {
                            pids.remove(pe.second)
                        }

                        Parsed("E", pe.first, pe.second, pe.second)
                    } else {
                        lineParser.parse(it)
                    }
                }


            }
            .filterNotNull()
            .filter {
                if (it is Parsed) {
                    !excludedTags.contains(it.tag.trim())
                } else {
                    false
                }
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
                    if (buffer.size == 0) {
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

            /*.runningFold(mutableListOf<LogLine>()) { acc, value ->
                if (acc.size > 1) {
                    val last = acc[acc.size - 1]
                    val previous = acc[acc.size - 2]

                    val r = when {
                        (last is Parsed) && (previous is Parsed) && (last.tag.contains(previous.tag)) -> false
                        else -> true
                    }

                    if (r) {
                        acc.clear()
                    }
                }

                val w = when (value) {
                    is Original -> {
                        //acc.add(value)
                        acc
                    }
                    is Parsed -> {

                        when {
                            acc.size == 0 -> {
                                //println("p $value\r")
                                acc.add(value)
                                mutableListOf()
                            }
                            (acc[0] as Parsed).tag.contains(value.tag) -> {
                                println("p $value\r")
                                val n = Parsed(value.level, "", value.owner, value.message)
                                acc.add(n)
                                mutableListOf()
                            }
                            else -> {
                                acc
                            }
                        }
                    }

                }
                //println(w)
                w
            }
            .flatMapConcat { it.asFlow() }*/
            .filter {
                if (it is Parsed) {
                    //pids.contains(it.owner)
                    logLevels.contains(it.level)
                } else {
                    true
                }
            }

            .withIndex()
            //.flowOn()
            .onCompletion { println("outer compl\r") }
    }

    fun <T> Flow<T>.bufferedTransform(
        shouldDrain: (List<T>, T) -> Boolean,
        transform: (List<T>, T) -> T
    ): Flow<T> = flow {
        val storage = mutableListOf<T>()

        collect { item ->
            val willDrain = shouldDrain(storage, item)
            if (willDrain) {
                storage.onEach { emit(it) }
                storage.clear()
            }

            val newItem = transform(storage, item)
            storage.add(newItem)
        }
    }

    fun <T, K> Flow<T>.groupToList(getKey: (T) -> K): Flow<Pair<K, List<T>>> = flow {
        val storage = mutableMapOf<K, MutableList<T>>()
        collect { t -> storage.getOrPut(getKey(t)) { mutableListOf() } += t }
        storage.forEach { (k, ts) -> emit(k to ts) }
    }

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

                println("zzzzzzzzzz $pids")

                //parsePs()

                //dumpCoroutines()
                scope.ensureActive()
                scope.cancel()
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

    private fun parseFg() {
        val out1 = Command("adb")
            .args("shell", "ps")
            .stdout(Stdio.Pipe)
            .spawn()

        val out = Command("adb")
            .args("shell", "dumpsys", "activity", "activities")
            .stdout(Stdio.Pipe)
            .spawn()

        val stdoutReader: Reader? = out.getChildStdout()

        while (true) {
            //ensureActive() -- call in scope
            val line2 = stdoutReader!!.readLine() ?: break

            val m = PID_LINE.matchEntire(line2)
            var r = false

            if (m != null) {
                val (line_pid, line_package) = m.destructured
                if (line_package.contains(p)) {
                    println("line: $line_pid $line_package\r")
                    pids.add(line_pid)
                    r = true
                }
            }
        }
    }

    private fun parsePs() {


        val out = Command("adb")
            .args("shell", "dumpsys", "activity", "activities")
            .stdout(Stdio.Pipe)
            .spawn()

        val stdoutReader: Reader? = out.getChildStdout()

        while (true) {
            //ensureActive() -- call in scope
            val line2 = stdoutReader!!.readLine() ?: break

            val m = FG_LINE.matchEntire(line2)
            var r = false

            if (m != null) {
                val (line_package) = m.destructured
                println("LP $line_package\r")
                p = line_package
//                if (line_package.contains(p)) {
//                    println("line: $line_pid $line_package\r")
//                    pids.add(line_pid)
//                    r = true
//                }
            }
        }

        /*stdoutReader!!
            .lines()
            .onEach { println("kkkkkk $it") }
            .filter {
                val m = PID_LINE.matchEntire(it)
                var r = false

                if (m != null) {
                    val (line_pid, line_package) = m.destructured
                    if (line_package.contains(p)) {
                        r = true
                    }
                }
                r
            }
            .onEach {
                println("GOT PID: $it")
                //pids.add(it)
            }*/

        out.wait()

        println("ksljdhflkdjshfdddddd")
    }
    //.*TaskRecord.*A[= ]([^ ^}]*)
    val FG_LINE = """^ +ResumedActivity: +ActivityRecord\{[^ ]* [^ ]* ([^ ^\/]*).*$""".toRegex()
    val PID_LINE = """^\w+\s+(\w+)\s+\w+\s+\w+\s+\w+\s+\w+\s+\w+\s+\w\s([\w|\.|\/]+)$""".toRegex()

    val PID_START = """^.*: Start proc ([a-zA-Z0-9._:]+) for ([a-z]+ [^:]+): pid=(\d+) uid=(\d+) gids=(.*)$""".toRegex()
    val PID_START_5_1 = """^.*: Start proc (\d+):([a-zA-Z0-9._:]+)/[a-z0-9]+ for (.*)$""".toRegex()
    val PID_START_DALVIK = """^E/dalvikvm\(\s*(\d+)\): >>>>> ([a-zA-Z0-9._:]+) \[ userId:0 \| appId:(\d+) \]$""".toRegex()
    val PID_KILL = """^.*Killing (\d+):([a-zA-Z0-9._:]+)/[^:]+: (.*)$""".toRegex() //.* to parse entire line, otherwise message only
    val PID_LEAVE = """^No longer want ([a-zA-Z0-9._:]+) \(pid (\d+)\): .*$""".toRegex()
    val PID_DEATH = """^Process ([a-zA-Z0-9._:]+) \(pid (\d+)\) has died.?$""".toRegex()

    var p = "com.norsedreki.multiplatform.identity.android"

    val pids = mutableSetOf<String>()

    private fun parseProcessDeath(line: String): Pair<String, String>? {
        val kill = PID_KILL.matchEntire(line)
        //println("zzzzzz $line")

        if (kill != null) {
            val (line_pid, line_package) = kill.destructured
            return line_package to line_pid
        }

        val leave = PID_LEAVE.matchEntire(line)

        if (leave != null) {
            val (line_package, line_pid)  = leave.destructured
            return line_package to line_pid
        }

        val death = PID_DEATH.matchEntire(line)
        if (death != null) {
            val (line_package, line_pid)  = death.destructured
            return line_package to line_pid
        }

        return null
    }



    private fun parseProcessStart(line: String): Pair<String, String>? {
        val m = PID_START_5_1.matchEntire(line)

        if (m != null) {
            val (line_pid, line_package, target) = m.destructured
            return line_package to line_pid
        }

        val m2 = PID_START.matchEntire(line)

        if (m2 != null) {
            val (line_package, target, line_pid, line_uid, line_gids) = m2.destructured
            return line_package to line_pid
        }

        val m3 = PID_START_DALVIK.matchEntire(line)
        if (m3 != null) {

            val (line_pid, line_package, line_uid) = m3.destructured
            return line_package to line_pid
        }

        //println("tried to parse $line \r")

        return null
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

        logSource.clear()
        val ci = LogcatState.InputCleared

        privateState.emit(ci)

        startupAll()
    }

    private suspend fun startupAll() {

        val filterLines = filterLines()

        val ci = LogcatState.CapturingInput(
            filterLines
                .onCompletion { println("COMPLETION $it\r") } //called when scope is cancelled as well
                .takeUntil(stopSubject),

            filterLines
                .filter {
                    var f = false

                    if (it.value is Parsed) {
                        if ((it.value as Parsed).level.contains("E")) {
                            f = true
                        }
                    }
                    f
                }
        )

        privateState.emit(ci)

        parsePs()
    }
}
