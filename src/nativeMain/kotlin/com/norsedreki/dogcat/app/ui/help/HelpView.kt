/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.help

import com.norsedreki.dogcat.app.ui.HasLifecycle
import com.norsedreki.dogcat.app.ui.Strings.HELP_WINDOW_TITLE
import kotlin.properties.Delegates
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.WINDOW
import ncurses.box
import ncurses.delwin
import ncurses.getmaxx
import ncurses.getmaxy
import ncurses.mvwaddstr
import ncurses.mvwin
import ncurses.newwin
import ncurses.stdscr
import ncurses.werase
import ncurses.wrefresh
import ncurses.wresize

@OptIn(ExperimentalForeignApi::class)
class HelpView : HasLifecycle {

    data class State(val text: List<String> = listOf())

    var state: State by Delegates.observable(State()) { _, _, newValue -> updateView(newValue) }

    private lateinit var window: CPointer<WINDOW>

    override suspend fun start() {
        val sy = getmaxy(stdscr) / 2
        val sx = getmaxx(stdscr) / 2

        window = newwin(1, 1, sy, sx)!!
        werase(window)
        wrefresh(window)
    }

    override suspend fun stop() {
        delwin(window)
    }

    private fun updateView(state: State) {
        val horizontalPadding = 4
        val verticalPadding = 2

        val maxWidth =
            maxOf(state.text.maxOf { it.length }, HELP_WINDOW_TITLE.length) + horizontalPadding * 2

        val height =
            state.text.size +
                verticalPadding * 2 +
                2 // add extra lines for the title and blank line

        val sy = getmaxy(stdscr)
        val sx = getmaxx(stdscr)

        val startY = (sy - height) / 2
        val startX = (sx - maxWidth) / 2

        wresize(window, height, maxWidth)
        mvwin(window, startY, startX)
        box(window, 0U, 0U)

        val titleStartX = (maxWidth - HELP_WINDOW_TITLE.length) / 2
        mvwaddstr(window, verticalPadding, titleStartX, HELP_WINDOW_TITLE)

        state.text.forEachIndexed { index, line ->
            mvwaddstr(
                window,
                index + verticalPadding + 2,
                horizontalPadding,
                line,
            )
        }

        wrefresh(window)
    }
}
