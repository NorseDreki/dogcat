package dogcat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

sealed interface LogcatState {

    data object WaitingInput : LogcatState

    data object InputCleared : LogcatState

    data class CapturingInput(
        val lines: Flow<IndexedValue<LogLine>>,

        val problems: Flow<IndexedValue<LogLine>> = emptyFlow(),

        //val appliedFilters: String,

        //val fatalException: String,
    ) : LogcatState

    data object Terminated : LogcatState
}
