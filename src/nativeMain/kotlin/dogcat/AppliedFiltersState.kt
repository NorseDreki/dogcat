package dogcat

import dogcat.LogFilter.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Logger
import kotlin.reflect.KClass

interface AppliedFiltersState {

    val applied: Flow<AppliedFilters>
}

typealias AppliedFilters = Map<KClass<out LogFilter>, Pair<LogFilter, Boolean>>

@OptIn(ExperimentalStdlibApi::class)
class InternalAppliedFiltersState : AppliedFiltersState {

    private val defaultFilters: AppliedFilters =
        mutableMapOf(
            Substring::class to (Substring("") to true),
            MinLogLevel::class to (MinLogLevel("V") to true)
        )

    private val appliedFiltersState = MutableStateFlow(defaultFilters)
    override val applied = appliedFiltersState.asStateFlow()

    suspend fun add(filter: LogFilter, enable: Boolean = true) {
        val next = appliedFiltersState.value + (filter::class to (filter to true))
        appliedFiltersState.emit(next)

        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Upsert filter: $next")
    }

    suspend fun removeFilter(filter: KClass<out LogFilter>) {
        val next = appliedFiltersState.value - filter
        appliedFiltersState.emit(next)

        Logger.d("[${currentCoroutineContext()[CoroutineDispatcher]}] After removing: $next ${currentCoroutineContext()[CoroutineDispatcher]}")
    }
}
