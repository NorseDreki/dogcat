/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.help

import com.norsedreki.dogcat.app.AppConfig.STATUS_VIEW_BOTTOM_MARGIN
import com.norsedreki.dogcat.app.ui.HasLifecycle
import kotlin.properties.Delegates
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.WINDOW
import ncurses.delwin
import ncurses.getmaxy
import ncurses.newwin
import ncurses.stdscr
import ncurses.wrefresh

@OptIn(ExperimentalForeignApi::class)
class HelpView : HasLifecycle {

    data class State(
        val packageName: String = "",
    )

    var state: State by Delegates.observable(State()) { _, _, newValue -> updateView(newValue) }

    private lateinit var window: CPointer<WINDOW>

    override suspend fun start() {
        val sy = getmaxy(stdscr)
        window = newwin(0, 0, sy - STATUS_VIEW_BOTTOM_MARGIN, 0)!!
    }

    override suspend fun stop() {
        delwin(window)
    }

    private fun updateView(state: State) {
        wrefresh(window)
    }
}
