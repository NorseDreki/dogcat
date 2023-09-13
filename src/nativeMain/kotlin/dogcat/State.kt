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

    fun upsertFilter(filter: LogFilter, enable: Boolean = true) {
        //appliedFiltersState.value = mutableMapOf()

    }

    override val appliedFilters = appliedFiltersState.asStateFlow()
}

typealias AppliedFilters = MutableMap<KClass<out LogFilter>, Pair<LogFilter, Boolean>>
