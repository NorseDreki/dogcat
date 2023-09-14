package dogcat

import Config
import dogcat.LogFilter.Substring
import platform.LogLineParser
import platform.LogcatBriefParser
import dogcat.LogcatState.WaitingInput
import filtering.Exclusions
import filtering.ProcessDeath
import filtering.ProcessStart
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

    private val appliedFilters = MutableStateFlow<MutableMap<KClass<out LogFilter>, LogFilter>>(mutableMapOf())

    private val stopSubject = MutableSharedFlow<Unit>()
    private val filterLine = MutableStateFlow<String>("")

    private val logLevels = mutableSetOf<String>("V", "D", "I", "W", "E") //+.WTF()? *.F

    val processStart = ProcessStart()
    val processEnd = ProcessDeath()

    var p = "com.norsedreki.multiplatform.identity.android"
    val pids = mutableSetOf<String>()

    private fun filterLines(): Flow<IndexedValue<LogLine>> {
        /*val sharedLines = logSource // deal with malformed UTF-8 'expected one more byte'
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
            .onCompletion { println("shared compl!\r") }*/


        return filterLine //logLevels.contains(it.level) //by tag //by time     -- both cases need to re-apply themselves upon every line
                //!Exclusions.excludedTags.contains(it.tag.trim())
                //pids.contains(it.owner) ??
            .flatMapLatest { filter ->
                logSource.lines().filter { it.contains(filter) }
                //sharedLines.filter { it.contains(filter) }
            }
            .map { trackProcesses(it) } //transform and highlight output
            // format message according to rules
            .filter { //filter which doesn't need to restart shared upstream
                if (it is Parsed) {
                    !Exclusions.excludedTags.contains(it.tag.trim())
                    //pids.contains(it.owner)

                } else {
                    true
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

    private fun trackProcesses(it: String): LogLine {
        val ps = processStart.parseProcessStart(it)

        return if (ps != null) {
            if (ps.first.contains(p)) {
                pids.add(ps.second)
            }
            Parsed("E", ps.first, ps.second, ps.second)
        } else {
            val pe = processEnd.parseProcessDeath(it)
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

    suspend operator fun invoke(cmd: LogcatCommands) {
        when (cmd) {
            is StartupAs -> startup(cmd)
            ClearLogs -> clearLogs()

            is FilterBy -> s.upsertFilter(cmd.filter)

            is ClearFilter -> clearFilter()
            //is Filter.ToggleLogLevel -> {}//s.upsertFilter()
            //is Filter.ByString -> s.upsertFilter(Substring(cmd.substring)) //filterWith(cmd)

            StopEverything -> {
                privateState.emit(LogcatState.Terminated)
                stopSubject.emit(Unit)

                //dumpCoroutines()
                scope.ensureActive()
                scope.cancel()
            }

        }
    }

    /*private fun filterByLogLevel(cmd: Filter.ToggleLogLevel) {
        // concurrent access protection?
        if (logLevels.contains(cmd.level)) {
            logLevels.remove(cmd.level)
        } else {
            logLevels.add(cmd.level)
        }
    }*/

    private fun clearFilter() {
    }

    private suspend fun filterWith(filter: Filter) {
        //if (filter is Filter.ByString) {
            //val filters = appliedFilters.value
            //filters[LogFilter.BySubstring::class] = LogFilter.BySubstring(filter.substring)

          //  s.upsertFilter(Substring(filter.substring))

            //appliedFilters.emit(filters)

            //filterLine.emit(filter.substring)
            //println("next filter ${filter.substring}")
        //}
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
        val pid = if (cmd is StartupAs.WithForegroundApp) {
            ForegroundProcess.parsePs()
        } else if (cmd is StartupAs.WithPackage) {
            p = cmd.packageName
            RunningProcesses().parsePs(cmd.packageName)
        } else {
            ""
        }

        if (pid.isNotEmpty()) {
            pids.add(pid)
        }

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
    }
}
