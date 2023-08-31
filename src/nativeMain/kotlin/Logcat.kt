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

    internal class ClosedException(val owner: FlowCollector<*>) :
        Exception("Flow was aborted, no more elements needed")

    internal fun ClosedException.checkOwnership(owner: FlowCollector<*>) {
        if (this.owner !== owner) throw this
    }

    public fun <T> Flow<T>.takeUntil(notifier: Flow<Any?>): Flow<T> = flow {
        try {
            coroutineScope {
                val job = launch(start = CoroutineStart.UNDISPATCHED) {
                    notifier.take(1).collect()
                    throw ClosedException(this@flow)
                }

                collect { emit(it) }
                job.cancel()
            }
        } catch (e: ClosedException) {
            e.checkOwnership(this@flow)
        }
    }


    val handler = CoroutineExceptionHandler { _, t -> println("999999 ${t.message}") }
    private val scope = CoroutineScope(dispatcherCpu + handler) // +Job +SupervisorJob +handler

    //State flow never completes. A call to Flow.collect on a state flow never completes normally, and neither does a coroutine started by the Flow.launchIn function. An active collector of a state flow is called a subscriber.

    //A subscriber of a shared flow can be cancelled. This usually happens when the scope in which the coroutine is running is cancelled. A subscriber to a shared flow is always cancellable, and checks for cancellation before each emission. Note that most terminal operators like Flow.toList would also not complete, when applied to a shared flow, but flow-truncating operators like Flow.take and Flow.takeWhile can be used on a shared flow to turn it into a completing one.

    //Note that most terminal operators like Flow.toList would also not complete, when applied to a shared flow, but flow-truncating operators like Flow.take and Flow.takeWhile can be used on a shared flow to turn it into a completing one.

    //SharedFlow cannot be closed like BroadcastChannel and can never represent a failure. All errors and completion signals should be explicitly materialized if needed.
    //Basically you will need to introduce a special object that you can emit from the shared flow to indicate that the flow has ended, using takeWhile at the consumer end can make them emit until that special object is received.

    //This is indeed what the doc suggests, but I don't think this is enough to replace BroadcastChannel. We need a low-level primitive that would allow performant code behaving like broadcast channel. Materializing the close event through wrapper objects really makes me reluctant to make the move. –
    //Joffrey
    // Oct 18, 2020 at 22:08
    //
    //In theory you could use an inline class and a symbol object to create a wrapper with minimal overhead. It would essentially end up being an implementation of the "poison pill" pattern. Overhead limited to a referential equals check for each item. –
    //Kiskae
    // Oct 19, 2020 at 20:20
    //
    //I see, the symbol object could help. But as far as I remember inline classes do create wrapper objects when used as generics, which is the case in a Flow<T>, so I'm a bit weary about the efficiency of the wrapper (in terms of gc pressure for instance in case of high number of messages) –
    //Joffrey
    // Oct 19, 2020 at 21:26
    //1
    //
    //You're right, it seems generics force a boxed type. In that case you'll probably end up like many of the internal kotlin classes and use Any to represent a union of ValueType | TerminationSymbol, where it can be either the next value or the terminal symbol object. With a wrapper around the entire system you'd be able to maintain some type safety.

    private val privateState = MutableStateFlow<LogcatState>(WaitingInput)
    //private val privateState = flowOf(WaitingInput).stateIn(scope)
    val state = privateState.asStateFlow()

    private val startSubject = MutableSharedFlow<Unit>(1)
    private val stopSubject = MutableSharedFlow<Unit>()
    private val filterLine = MutableStateFlow<String>("")
    private val logLevels = mutableSetOf<String>("V", "D", "I", "W", "E") //+.WTF()?

    private val sharedLines = //startSubject //should not be needed
        //beware of implicit distinctuntilchanged
        /*.flatMapLatest {
            println("to start logcat command")*/
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
        //}
        .shareIn(
            scope,
            //SharingStarted.WhileSubscribed()
            SharingStarted.Eagerly, //should use lazy or subscribed instead
            50000,
        )
        //.onEach { currentCoroutineContext().ensureActive() }
        //.onCompletion { println("shared completed") }

    private val filteredLines = filterLine
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
        .onCompletion { println("COMPLETION") }
        .takeUntil(stopSubject)

        //.onCompletion { println("filtered completed") }
        //.flowOn(scope.coroutineContext)
        //.cancellable()

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

            logSource.clear()

            val ci = LogcatState.InputCleared

            privateState.emit(ci)

            stopSubject.emit(Unit)

            startupAll()
        }
    }

    private fun startupAll() {
        scope.launch {
            println("jhkjhkjh  11111")
            startSubject.emit(Unit)

            val ci = LogcatState.CapturingInput(
                filteredLines
            )

            println("prepare to emit capturing")
            privateState.emit(ci)

            println("emitted capturing")
        }
    }
}
