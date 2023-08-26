import kotlinx.coroutines.flow.Flow

sealed interface LogcatState {

    data object WaitingInput : LogcatState

    data object InputCleared: LogcatState

    data object Waiting : LogcatState

    data class CapturingInput(
        val lines: Flow<LogLine>,

        //val appliedFilters: String,

        //val warningsAndErrors: String,

        //val fatalException: String,

        //val snapScrolling: Boolean
    ) : LogcatState
}
