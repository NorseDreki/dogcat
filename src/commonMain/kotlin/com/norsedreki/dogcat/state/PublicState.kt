package com.norsedreki.dogcat.state

import com.norsedreki.dogcat.LogLine
import kotlinx.coroutines.flow.Flow

sealed interface PublicState {

    data class Active(
        val lines: Flow<IndexedValue<LogLine>>,
        //looks like no need for flow
        //or should have nested state, appliedfiltersstate
        val applied: Flow<AppliedFilters>,
        val device: Device,

        ) : PublicState//, AppliedFiltersState

    data object Inactive : PublicState

    data object Terminated : PublicState
}

data class Device(
    val label: String,
    val isOnline: Flow<Boolean>
)
