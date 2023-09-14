package dogcat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

interface State {


    //private val appliedFilters = MutableStateFlow<MutableMap<KClass<out LogFilter>, LogFilter>>(mutableMapOf())

    val appliedFilters: Flow<AppliedFilters>

}


class InternalState : State {

    private val appliedFiltersState = MutableStateFlow<AppliedFilters>(mutableMapOf())
    override val appliedFilters = appliedFiltersState.asStateFlow()

    suspend fun upsertFilter(filter: LogFilter, enable: Boolean = true) {
        println("sub ${appliedFiltersState.subscriptionCount.value}")
        val v = appliedFiltersState.value
        v[filter::class] = filter to true

        println("current filters $v")
        println("sub ${appliedFiltersState.subscriptionCount.value}")


        appliedFiltersState.emit(mutableMapOf(LogFilter.MinLogLevel::class to (LogFilter.MinLogLevel("E") to true)))
        println("sub ${appliedFiltersState.subscriptionCount.value}")

        println("zzzz ${appliedFiltersState.value}")
    }


}

typealias AppliedFilters = MutableMap<KClass<out LogFilter>, Pair<LogFilter, Boolean>>
