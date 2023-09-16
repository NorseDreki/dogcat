package dogcat

import kotlinx.coroutines.flow.Flow

sealed interface LogcatState {
    data object WaitingInput : LogcatState

    data object InputCleared : LogcatState

    data class CapturingInput(
        val lines: Flow<IndexedValue<LogLine>>,

        val appliedFilters: Flow<AppliedFilters>,

    ) : LogcatState

    data object Terminated : LogcatState
}
