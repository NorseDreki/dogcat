package dogcat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Logger
import kotlin.reflect.KClass

interface State {

    val appliedFilters: Flow<AppliedFilters>
}

class InternalState : State {

    private val appliedFiltersState = MutableStateFlow<AppliedFilters>(mutableMapOf())
    override val appliedFilters = appliedFiltersState.asStateFlow()

    private val af: AppliedFilters = mutableMapOf()

    suspend fun upsertFilter(filter: LogFilter, enable: Boolean = true) {
        af[filter::class] = filter to enable
        appliedFiltersState.emit(af)


        Logger.d("sub ${appliedFiltersState.subscriptionCount.value} $af")

        Logger.d("zzzz ${appliedFiltersState.value}")
    }
}

typealias AppliedFilters = MutableMap<KClass<out LogFilter>, Pair<LogFilter, Boolean>>
