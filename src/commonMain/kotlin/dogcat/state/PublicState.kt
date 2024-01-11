package dogcat.state

import dogcat.LogLine
import kotlinx.coroutines.flow.Flow

sealed interface PublicState {
    data object WaitingInput : PublicState

    data object InputCleared : PublicState

    data class CapturingInput(
        val lines: Flow<IndexedValue<LogLine>>,

        override val applied: Flow<AppliedFilters>,

        val deviceName: String?,

        val heartbeat: Flow<Boolean>


    ) : PublicState, AppliedFiltersState

    data object Stopped : PublicState
}
