package dogcat

import flow.bufferedTransform
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.LogcatBriefParser
import platform.Logger

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class LogLines(
    val logLinesSource: LogLinesSource,
    private val lineParser: LogLineParser,
    private val s: InternalAppliedFiltersState,
    private val dispatcherCpu: CoroutineDispatcher = Dispatchers.Default,
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO,
) {

    val handler = CoroutineExceptionHandler { _, t -> Logger.d(" CATCH! ${t.message}\r") }
    private lateinit var scope: CoroutineScope

    suspend fun filterLines(): Flow<IndexedValue<LogLine>> {
        if (this::scope.isInitialized) {
            Logger.d("[${currentCoroutineContext()[CoroutineDispatcher]}] !!!!! cancelling scope")
            withContext(dispatcherCpu) {
                scope.cancel()
            }
        }

        scope = CoroutineScope(dispatcherCpu + handler + kotlinx.coroutines.Job()) // +Job +SupervisorJob +handler

        val sharedLines = logLinesSource // deal with malformed UTF-8 'expected one more byte'
            .lines()
            /*.retry(3) { e ->
                val shallRetry = e is RuntimeException
                if (shallRetry) delay(100)
                Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] retrying... $shallRetry\r")
                shallRetry
            }*/
            //.takeUntil(stopSubject)
            .onCompletion { cause -> if (cause == null) emit("[${(currentCoroutineContext()[CoroutineDispatcher])}] INPUT HAS EXITED") else Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] EXIT COMPLETE $cause\r") }
            .onStart { Logger.d("[${currentCoroutineContext()[CoroutineDispatcher]}] Start subscription to logLinesSource\r") }
            .shareIn(
                scope,
                SharingStarted.Lazily,
                Config.LogLinesBufferCount,
            )
            .onSubscription { Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Subscribing to shareIn\r") }
            .onCompletion { Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Subscription to shareIn completed\r") }

        return s.applied
            .flatMapConcat {
                Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Applied filters flat map concat")
                it.values.asFlow()
            }
            .map { it.first }
            .filterIsInstance<LogFilter.Substring>()
            .flatMapLatest { filter ->
                Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Shared lines flat map latest")
                sharedLines.filter { it.contains(filter.substring) }
            }
            .map {
                lineParser.parse(it)
            }
            .bufferedTransform(
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
            )
            .withIndex()
            //.flowOn()
            .onCompletion { Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Full LogLines chain completed\r") }
    }
}
