package dogcat

import kotlinx.coroutines.flow.Flow

sealed interface PublicState {
    data object WaitingInput : PublicState

    data object InputCleared : PublicState

    data class CapturingInput(
        val lines: Flow<IndexedValue<LogLine>>,
        override val applied: Flow<AppliedFilters>
    ) : PublicState, AppliedFiltersState

    data object Stopped : PublicState
}


interface State {

    val st: Flow<PublicState>
}


class InternalState(
    //val appliedFiltersState: AppliedFiltersState
) : State {
    override val st: Flow<PublicState>
        get() = TODO("Not yet implemented")

    fun aaa() {}


    fun nextState(s: PublicState) {

    }
}
