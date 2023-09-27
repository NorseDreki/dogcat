package dogcat

import kotlinx.coroutines.flow.Flow

sealed interface PublicState {
    data object WaitingInput : PublicState

    data object InputCleared : PublicState

    data class CapturingInput(
        val lines: Flow<IndexedValue<LogLine>>,

        override val applied: Flow<AppliedFilters>,

        val deviceName: String?
    ) : PublicState, AppliedFiltersState

    data object Stopped : PublicState
}
