package dogcat

import bufferedTransform
import dogcat.LogFilter.Substring
import dogcat.state.AppliedFiltersState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import logger.Logger
import logger.context

@OptIn(ExperimentalCoroutinesApi::class)
class LogLines(
    private val lineParser: LogLineParser,
    private val filtersState: AppliedFiltersState,
    private val shell: Shell,
    private val dispatcherCpu: CoroutineDispatcher,
) {
    private val handler = CoroutineExceptionHandler { _, t -> Logger.d("!!!!!!11111111 CATCH! ${t.message}\r") }
    private lateinit var scope: CoroutineScope
    private lateinit var sharedLines: Flow<String>

    suspend fun capture(restartSource: Boolean = true): Flow<IndexedValue<LogLine>> {
        if (restartSource) {
            if (this::scope.isInitialized) {
                Logger.d("${context()} !!!!! cancelling scope ${scope.coroutineContext[Job]}")

                scope.cancel()
            }
            scope = CoroutineScope(dispatcherCpu + handler + Job())
            sharedLines = createSharedLines()
            Logger.d("${context()} created shared lines in capture $sharedLines")
        }

        var i = 0
        return filtersState.applied
            .flatMapConcat {
                Logger.d("${context()} Applied filters flat map concat")
                it.values.asFlow()
            }
            .filterIsInstance<Substring>()
            //never called, always restarted
            .distinctUntilChanged { old, new ->
                Logger.d("] Distinct? $old $new")
                old.substring == new.substring
            }
            .flatMapLatest { filter ->
                Logger.d("${context()} flat map shared lines")
                val f = sharedLines
                    .filter { it.contains(filter.substring, ignoreCase = true) }
                f
            }
            .map {
                lineParser.parse(it)
            }
            .bufferedTransform(
                { buffer, item ->
                    when (item) {
                        is Brief -> {
                            when {
                                buffer.isNotEmpty() -> {
                                    val previous = buffer[0]
                                    when {
                                        //previous == null -> false
                                        (previous as? Brief)?.tag?.contains(item.tag) ?: false -> false
                                        //item.tag.contains((previous as? Brief).tag) -> false
                                        else -> true
                                    }
                                }

                                else -> false
                            }
                        }

                        is Unparseable -> false // Pass through Unparseable items
                    }
                },
                { buffer, item ->
                    when (item) {
                        is Brief -> {
                            if (buffer.isEmpty()) {
                                item
                            } else {
                                Brief(item.level, "", item.owner, item.message)
                            }
                        }

                        is Unparseable -> item // Pass through Unparseable items
                    }
                }
            )
            .withIndex()
            .onEach {
                if (i < 15) {
                    Logger.d("${context()} csl ${it.index}")
                    i++
                }
            }
            .onCompletion { Logger.d("${context()} (2) COMPLETED Full LogLines chain\r") }
            .flowOn(dispatcherCpu)
    }

    suspend fun stop() {
        scope.cancel()
    }

    private fun createSharedLines(): Flow<String> {
        val af = filtersState.applied.value

        val minLogLevel =
            af[LogFilter.MinLogLevel::class]?.let { "*:${(it as LogFilter.MinLogLevel).logLevel}" } ?: ""
        val pkgE = true
        val userId = if (pkgE) {
            af[LogFilter.ByPackage::class]?.let { "--uid=${(it as LogFilter.ByPackage).resolvedUserId}" }
                ?: ""
        } else {
            ""
        }

        var i = 0

        return shell
            .lines(minLogLevel, userId)
            .onStart { Logger.d("${context()} Start subscription to logLinesSource") }
            .shareIn(
                scope,
                SharingStarted.Lazily,
                DogcatConfig.MAX_LOG_LINES,
            )
            .onSubscription { Logger.d("${context()} Subscribing to shareIn") }
            .onCompletion { Logger.d("${context()} (3) COMPLETED Subscription to shareIn") }
    }
}
