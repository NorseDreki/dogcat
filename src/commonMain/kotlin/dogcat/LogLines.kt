package dogcat

import bufferedTransform
import dogcat.state.DefaultAppliedFiltersState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import logger.Logger
import logger.context

@OptIn(ExperimentalCoroutinesApi::class)
class LogLines(
    private val lineParser: LogLineParser,
    private val s: DefaultAppliedFiltersState,
    private val shell: Shell,
    private val dispatcherCpu: CoroutineDispatcher = Dispatchers.Default,
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO,
) {

    private val handler = CoroutineExceptionHandler { _, t -> Logger.d("!!!!!!11111111 CATCH! ${t.message}\r") }
    private lateinit var scope: CoroutineScope
    private lateinit var sharedLines: Flow<String>

    suspend fun capture(restartSource: Boolean = true): Flow<IndexedValue<LogLine>> {
        if (restartSource) {
            if (this::scope.isInitialized) {
                Logger.d("${context()} !!!!! cancelling scope ${scope.coroutineContext[Job]}")
                withContext(dispatcherCpu) {

                    scope.cancel()
                }
                //scope.cancel()
            }

            scope = CoroutineScope(dispatcherIo + handler + Job())
            sharedLines = createSharedLines()
        }

        return s.applied
            .flatMapConcat {
                Logger.d("${context()} Applied filters flat map concat")
                it.values.asFlow()
            }
            .filterIsInstance<LogFilter.Substring>()
            //never called, always restarted
            .distinctUntilChanged { old, new ->
                Logger.d("] Distinct? $old $new")
                old.substring == new.substring
            }
            .flatMapLatest { filter ->
                val f = sharedLines
                    .filter { it.contains(filter.substring, ignoreCase = true) }
                    .onEach {
                        //logger.Logger.d("${context()} !!!!!!!!!!!!  '${filter.substring}' $restartSource Shared lines flat map latest")
                    }

                f
            }
            .map {
                lineParser.parse(it)
            }
            .filterIsInstance<Brief>()
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
                        Brief(item.level, "", item.owner, item.message)
                    }
                }
            )
            .withIndex()
            //.flowOn()
            .onCompletion { Logger.d("${context()} (2) COMPLETED Full LogLines chain\r") }
            .flowOn(dispatcherCpu)
    }

    private fun createSharedLines(): Flow<String> {
        val af = s.applied.value

        val minLogLevel =
            af[LogFilter.MinLogLevel::class]?.let { "*:${(it as LogFilter.MinLogLevel).logLevel}" } ?: ""
        val pkgE = true
        val userId = if (pkgE) {
            af[LogFilter.ByPackage::class]?.let { "--uid=${(it as LogFilter.ByPackage).resolvedUserId}" }
                ?: ""
        } else {
            ""
        }

        //Unparseable log line: '--------- beginning of kernel'
        //>>>>>>>>>>>>>>>>> inner catch kotlinx.coroutines.JobCancellationException: ScopeCoroutine is cancelling; job=ScopeCoroutine{Cancelling}@c09b290  ScopeCoroutine{Cancelling}@c09b290
        //>>>>>>>>>>>>  [Dispatchers.IO] !!!!!!!!! onCompletion true
        //||||||||||||||||||||||||||||||||||||||||||||| An error occurred: com.kgit2.kommand.exception.KommandException: whoa
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! null

        //[Dispatchers.IO] (4) COMPLETED, loglinessource.lines null

        //Unparseable log line: '[Dispatchers.IO] INPUT HAS EXITED'
        //Unparseable log line: '[Dispatchers.IO] INPUT HAS EXITED'

        return shell
            .lines(minLogLevel, userId)
            //retry?
            .onCompletion { cause ->
                Logger.d("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! $cause\n")

                if (cause == null) {
                    Logger.d("${context()} (4) COMPLETED, loglinessource.lines $cause\n")
                    emit("--- ADB has terminated, no longer waiting for input") //will suspend
                    Logger.d("${context()} (4) COMPLETED emitted, loglinessource.lines $cause\n")
                } else {
                    Logger.d("${context()} EXIT COMPLETE $cause\r")
                }
            }
            .catch { cause ->
                Logger.d("|||||||||||||||||||||||||||||||||||||||||||||||||||  Flow was cancelled, cleaning up resources...")
        }
            .onStart { Logger.d("${context()} Start subscription to logLinesSource\r") }
            .shareIn(
                scope,
                SharingStarted.Lazily,
                DogcatConfig.MAX_LOG_LINES,
            )
            .onSubscription { Logger.d("${context()} Subscribing to shareIn\r") }
            .onCompletion { Logger.d("${context()} (3) COMPLETED Subscription to shareIn\r") }
    }
}
