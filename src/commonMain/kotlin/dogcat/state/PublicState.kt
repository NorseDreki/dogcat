package dogcat.state

import dogcat.LogLine
import kotlinx.coroutines.flow.Flow

sealed interface PublicState {

    data class Active(
        val lines: Flow<IndexedValue<LogLine>>,
        val applied: Flow<AppliedFilters>,
        val deviceName: String?,
        val heartbeat: Flow<Boolean>
    ) : PublicState//, AppliedFiltersState

    data object Inactive : PublicState

    data object Terminated : PublicState
}
