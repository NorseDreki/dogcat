package com.norsedreki.dogcat

import com.norsedreki.bufferedTransform
import com.norsedreki.dogcat.DogcatConfig.MAX_LOG_LINES
import com.norsedreki.dogcat.LogFilter.*
import com.norsedreki.dogcat.state.LogFiltersState
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class LogLines(
    private val lineParser: LogLineParser,
    private val filtersState: LogFiltersState,
    private val shell: Shell,
    private val dispatcherCpu: CoroutineDispatcher,
) {
    private lateinit var scope: CoroutineScope
    private lateinit var sharedLines: Flow<String>

    suspend fun capture(restartSource: Boolean = true): Flow<IndexedValue<LogLine>> {
        if (restartSource) {
            if (this::scope.isInitialized) {
                Logger.d("${context()} Cancel coroutine scope for log lines collection")

                scope.cancel()
            }

            // We need a dedicated job otherwise 'scope.cancel()' would cancel parent scope as well
            scope = CoroutineScope(coroutineContext + dispatcherCpu + Job())
            sharedLines = createSharedLines()

            Logger.d("${context()} Created shared log lines by re-starting ADB logcat")
        }

        return filtersState.state
            .flatMapConcat {
                it.values.asFlow()
            }
            .filterIsInstance<Substring>()
            .flatMapLatest { filter ->
                sharedLines.filter {
                    it.contains(filter.substring, ignoreCase = true)
                }
            }
            .map {
                lineParser.parse(it)
            }
            .bufferedTransform(
                shouldEmptyBuffer(),
                transformItem()
            )
            .withIndex()
            .onCompletion {
                Logger.d("${context()} COMPLETION (2): Full log lines pipeline")
            }
            .flowOn(dispatcherCpu)
    }

    suspend fun stop() {
        scope.cancel()
    }

    private fun transformItem() = { buffer: List<LogLine>, item: LogLine ->
        when (item) {
            is Brief -> {
                if (buffer.isEmpty()) {
                    item
                } else {
                    // Do not display tag if it's the same as in previous item
                    Brief(item.level, "", item.owner, item.message)
                }
            }

            is Unparseable -> item
        }
    }

    private fun shouldEmptyBuffer() = { buffer: List<LogLine>, item: LogLine ->
        when (item) {
            is Brief -> {
                when {
                    buffer.isNotEmpty() -> {
                        // All items in buffer have same tags, otherwise buffer is drained
                        val anyPreviousItem = buffer[0]

                        val doesTagOfPreviousItemMatchCurrent =
                            (anyPreviousItem as? Brief)?.tag?.contains(item.tag) ?: false

                        when {
                            // Don't drain buffer since each item has the same tag
                            doesTagOfPreviousItemMatchCurrent -> false
                            // Drain buffer since there is a new tag
                            else -> true
                        }
                    }

                    else -> false
                }
            }
            // We don't group 'Unparseable' since it doesn't have tag
            is Unparseable -> false
        }
    }

    private fun createSharedLines(): Flow<String> {
        val filters = filtersState.state.value

        val minLogLevel =
            filters[MinLogLevel::class]?.let {
                "*:${(it as MinLogLevel).logLevel}"
            } ?: ""

        val appId =
            filters[ByPackage::class]?.let {
                "--uid=${(it as ByPackage).appId}"
            } ?: ""

        return shell
            .logLines(minLogLevel, appId)
            .shareIn(
                scope,
                SharingStarted.Lazily,
                MAX_LOG_LINES,
            )
            .onCompletion {
                Logger.d(
                    "${context()} COMPLETION (3): ADB logcat log lines shred by 'shareIn")
            }
    }
}