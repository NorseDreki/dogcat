package dogcat

import Config.DEFAULT_MIN_LOG_LEVEL
import dogcat.LogFilter.MinLogLevel
import dogcat.LogFilter.Substring
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import Logger
import kotlin.reflect.KClass

interface AppliedFiltersState {
    val applied: Flow<AppliedFilters>
}

typealias AppliedFilters = Map<KClass<out LogFilter>, LogFilter>

@OptIn(ExperimentalStdlibApi::class)
class InternalAppliedFiltersState : AppliedFiltersState {

    private val defaultFilters: AppliedFilters =
        mapOf(
            Substring::class to Substring(""),
            MinLogLevel::class to MinLogLevel(DEFAULT_MIN_LOG_LEVEL) //use log level as enum
        )

    private val appliedFiltersState = MutableStateFlow(defaultFilters)
    override val applied = appliedFiltersState.asStateFlow()

    suspend fun apply(filter: LogFilter) {
        val next = appliedFiltersState.value + (filter::class to filter)
        appliedFiltersState.emit(next)

        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Upsert filter: $next")
    }

    suspend fun reset(filterClass: KClass<out LogFilter>) {
        val default = defaultFilters[filterClass]

        val next = if (default != null) {
            appliedFiltersState.value + (filterClass to default)
        } else {
            appliedFiltersState.value - filterClass
        }
        appliedFiltersState.emit(next)

        Logger.d("[${currentCoroutineContext()[CoroutineDispatcher]}] After removing: $next")
    }
}
