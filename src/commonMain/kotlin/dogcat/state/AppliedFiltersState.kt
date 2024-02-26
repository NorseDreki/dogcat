package dogcat.state

import dogcat.DogcatConfig.DEFAULT_MIN_LOG_LEVEL
import dogcat.LogFilter
import dogcat.LogFilter.MinLogLevel
import dogcat.LogFilter.Substring
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import logger.Logger
import logger.context
import kotlin.reflect.KClass


typealias AppliedFilters = Map<KClass<out LogFilter>, LogFilter>

interface AppliedFiltersState {
    val applied: StateFlow<AppliedFilters>

    suspend fun apply(filter: LogFilter)

    suspend fun reset(filterClass: KClass<out LogFilter>)
}

class DefaultAppliedFiltersState : AppliedFiltersState {

    private val defaultFilters: AppliedFilters =
        mapOf(
            Substring::class to Substring(""),
            MinLogLevel::class to MinLogLevel(DEFAULT_MIN_LOG_LEVEL)
        )

    private val appliedFiltersState = MutableStateFlow(defaultFilters)
    override val applied = appliedFiltersState.asStateFlow()

    override suspend fun apply(filter: LogFilter) {
        val next = appliedFiltersState.value + (filter::class to filter)
        appliedFiltersState.emit(next)
    }

    override suspend fun reset(filterClass: KClass<out LogFilter>) {
        val default = defaultFilters[filterClass]

        val next = if (default != null) {
            appliedFiltersState.value + (filterClass to default)
        } else {
            appliedFiltersState.value - filterClass
        }

        appliedFiltersState.emit(next)
    }
}
