package dogcat

import dogcat.LogFilter.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.yield
import platform.Logger
import kotlin.reflect.KClass

interface Query {

    val appliedFilters: Flow<AppliedFilters>
}

class InternalQuery : Query {

    private val defaultFilters: AppliedFilters =
        mutableMapOf(
            Substring::class to (Substring("") to true),
            MinLogLevel::class to (MinLogLevel("V") to true)
        )

    private val appliedFiltersState = MutableStateFlow(defaultFilters)
    override val appliedFilters = appliedFiltersState.asStateFlow()

    suspend fun upsertFilter(filter: LogFilter, enable: Boolean = true) {
        val next = appliedFiltersState.value + (filter::class to (filter to true))
        appliedFiltersState.emit(next)

        Logger.d("Upsert filter: ${appliedFiltersState.subscriptionCount.value} $next")
    }

    suspend fun removeFilter(filter: KClass<out LogFilter>) {
        val a = appliedFiltersState.value - filter
        appliedFiltersState.emit(a)

        Logger.d("After removing: ${appliedFiltersState.value}")
    }
}

typealias AppliedFilters = Map<KClass<out LogFilter>, Pair<LogFilter, Boolean>>
