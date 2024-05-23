/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app

import com.norsedreki.dogcat.LogFilter.ByPackage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppStateHolder(
    val autoscroll: Boolean,
    val packageFilter: Pair<ByPackage?, Boolean>,
    val userInputLocation: Pair<Int, Int>,
    val isCursorHeld: Boolean,
    val isUiHeld: Boolean
)

interface AppState {

    val state: StateFlow<AppStateHolder>

    fun autoscroll(on: Boolean)

    fun filterByPackage(f: ByPackage?, enable: Boolean)

    fun setUserInputLocation(x: Int, y: Int)

    fun holdCursor(hold: Boolean)

    fun holdUi(hold: Boolean)
}

class InternalAppState : AppState {

    override val state =
        MutableStateFlow(
            AppStateHolder(
                false,
                null to false,
                0 to 0,
                false,
                false,
            ),
        )

    override fun autoscroll(on: Boolean) {
        state.value = state.value.copy(autoscroll = on)
    }

    override fun filterByPackage(f: ByPackage?, enable: Boolean) {
        state.value = state.value.copy(packageFilter = f to enable)
    }

    override fun setUserInputLocation(x: Int, y: Int) {
        state.value = state.value.copy(userInputLocation = x to y)
    }

    override fun holdCursor(hold: Boolean) {
        state.value = state.value.copy(isCursorHeld = hold)
    }

    override fun holdUi(hold: Boolean) {
        state.value = state.value.copy(isUiHeld = hold)
    }
}
