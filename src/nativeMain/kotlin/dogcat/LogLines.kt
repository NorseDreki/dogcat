package dogcat

import LogFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.Logger

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class LogLines(
    val logLinesSource: LogLinesSource,
    private val lineParser: LogLineParser,
    private val s: InternalAppliedFiltersState,
    private val dispatcherCpu: CoroutineDispatcher = Dispatchers.Default,
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO,
) {

    val handler = CoroutineExceptionHandler { _, t -> Logger.d("!!!!!!11111111 CATCH! ${t.message}\r") }
    private lateinit var scope: CoroutineScope
    private lateinit var sharedLines: Flow<String>

    suspend fun capture(restartSource: Boolean = true): Flow<IndexedValue<LogLine>> {
        if (restartSource) {
            if (this::scope.isInitialized) {
                Logger.d("[${currentCoroutineContext()[CoroutineDispatcher]}] !!!!! cancelling scope")
                withContext(dispatcherCpu) {
                    scope.cancel()
                }
            }

            scope = CoroutineScope(dispatcherIo + handler + Job())
            sharedLines = createSharedLines()
        }

        return s.applied
            .flatMapConcat {
                Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Applied filters flat map concat")
                it.values.asFlow()
            }
            .filterIsInstance<LogFilter.Substring>()
            .flatMapLatest { filter ->
                val f = sharedLines
                    .filter { it.contains(filter.substring, ignoreCase = true) }
                    .onEach {
                        //Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] !!!!!!!!!!!!  '${filter.substring}' $restartSource Shared lines flat map latest")
                    }

                f
            }
            .map {
                lineParser.parse(it)
            }
            /*.bufferedTransform(
                { buffer, item ->
                    when {
                        buffer.isNotEmpty() -> {
                            val previous = buffer[0]
                            when {
                                item.tag.contains(previous.tag) -> false
                                else -> true
                            }
                        }
                        else -> false
                    }
                },
                { buffer, item ->
                    if (buffer.isEmpty()) {
                        item
                    } else {
                        LogLine(item.level, "", item.owner, item.message)
                    }
                }
            )*/
            .withIndex()
            //.flowOn()
            .onCompletion { Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] (2) COMPLETED Full LogLines chain\r") }
            .flowOn(dispatcherCpu)
    }

    private fun createSharedLines() = logLinesSource
        .lines()
        .onCompletion { cause ->
            if (cause == null) {
                Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] (4) COMPLETED, loglinessource.lines $cause\r")
                emit("[${(currentCoroutineContext()[CoroutineDispatcher])}] INPUT HAS EXITED")
            } else {
                Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] EXIT COMPLETE $cause\r")
            }
        }
        .onStart { Logger.d("[${currentCoroutineContext()[CoroutineDispatcher]}] Start subscription to logLinesSource\r") }
        .shareIn(
            scope,
            SharingStarted.Lazily,
            Config.LogLinesBufferCount,
        )
        .onSubscription { Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Subscribing to shareIn\r") }
        .onCompletion { Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] (3) COMPLETED Subscription to shareIn\r") }
}

interface Debug {
    val enabled: Boolean
    fun log(message: String)

    object Disabled : Debug {
        override val enabled get() = false
        override fun log(message: String) = Unit
    }

    object Console : Debug {
        override val enabled get() = true
        override fun log(message: String) = println("[DEBUG] $message")
    }
}

inline fun Debug.log(message: () -> String) {
    if (enabled) {
        log(message())
    }
}