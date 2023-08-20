import kotlinx.coroutines.flow.Flow

sealed interface LogcatState {

    data object WaitingForDevice

    data object Empty // Cleared

    data class Running(
        val lines: Flow<String>,

        val appliedFilters: String,

        val warningsAndErrors: String,

        val fatalException: String,

        val snapScrolling: Boolean
    )
}
