package dogcat

import flow.bufferedTransform
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.LogcatBriefParser
import platform.Logger

@OptIn(ExperimentalCoroutinesApi::class)
class LogLines(
    val logLinesSource: LogLinesSource,
    private val lineParser: LogLineParser,
    private val s: InternalAppliedFiltersState,
    private val dispatcherCpu: CoroutineDispatcher = Dispatchers.Default,
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO,
) {

    val handler = CoroutineExceptionHandler { _, t -> Logger.d("CATCH! ${t.message}\r") }
    private val scope = CoroutineScope(dispatcherCpu + handler + Job()) // +Job +SupervisorJob +handler

    fun filterLines(): Flow<IndexedValue<LogLine>> {
        val sharedLines = logLinesSource // deal with malformed UTF-8 'expected one more byte'
            .lines()
            .retry(3) { e ->
                val shallRetry = e is RuntimeException
                if (shallRetry) delay(100)
                Logger.d("retrying... $shallRetry\r")
                shallRetry
            }
            //.takeUntil(stopSubject)
            .onCompletion { cause -> if (cause == null) emit("INPUT HAS EXITED") else Logger.d("EXIT COMPLETE $cause\r") }
            .onStart { Logger.d("start logcat lines\r") }
            .shareIn(
                scope,
                SharingStarted.Lazily,
                Config.LogLinesBufferCount,
            )
            .onSubscription { Logger.d("subscr to shared lines\r") }
            .onCompletion { Logger.d("shared compl!\r") }

        return s.applied
            .flatMapConcat { it.values.asFlow() }
            .map { it.first }
            .filterIsInstance<LogFilter.Substring>()
            .flatMapLatest { filter ->
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
            .onCompletion { Logger.d("outer compl\r") }
    }
}
