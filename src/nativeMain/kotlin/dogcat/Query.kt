package dogcat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Logger
import kotlin.reflect.KClass

interface Query {

    val appliedFilters: Flow<AppliedFilters>
}

class InternalQuery : Query {

    private val appliedFiltersState = MutableStateFlow<AppliedFilters>(mutableMapOf())
    override val appliedFilters = appliedFiltersState.asStateFlow()

    private val af: AppliedFilters = mutableMapOf(LogFilter.Substring::class to (LogFilter.Substring("") to true))

    suspend fun upsertFilter(filter: LogFilter, enable: Boolean = true) {
        af[filter::class] = filter to enable
        appliedFiltersState.emit(af)


        Logger.d("sub ${appliedFiltersState.subscriptionCount.value} $af")

        Logger.d("zzzz ${appliedFiltersState.value}")
    }

    suspend fun removeFilter(filter: KClass<out LogFilter>) {
        af.remove(filter)
        appliedFiltersState.emit(af)
    }
}

typealias AppliedFilters = MutableMap<KClass<out LogFilter>, Pair<LogFilter, Boolean>>
