/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.logLines

import com.norsedreki.dogcat.DogcatConfig.MAX_LOG_LINES
import com.norsedreki.dogcat.app.AppConfig.DEFAULT_TAG_WIDTH
import com.norsedreki.dogcat.app.AppConfig.LOG_LINES_VIEW_BOTTOM_MARGIN
import com.norsedreki.dogcat.app.ui.HasLifecycle
import com.norsedreki.logger.Logger
import kotlin.math.min
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.WINDOW
import ncurses.curs_set
import ncurses.delwin
import ncurses.getmaxx
import ncurses.getmaxy
import ncurses.newpad
import ncurses.prefresh
import ncurses.scrollok
import ncurses.stdscr
import ncurses.wclear
import ncurses.wmove
import ncurses.wrefresh

@OptIn(ExperimentalForeignApi::class)
class LogLinesView : HasLifecycle {

    data class State(
        val autoscroll: Boolean = false,
        val overscroll: Boolean = false,
        val showLineNumbers: Boolean = false,
        val tagWidth: Int = DEFAULT_TAG_WIDTH,
        val isCursorHeld: Boolean = false,
        val cursorReturnLocation: Pair<Int, Int>? = null
    )

    var state = State()

    internal var firstVisibleLine = 0
    internal var linesCount = 0

    internal lateinit var pad: CPointer<WINDOW>

    internal val endX by lazy { getmaxx(stdscr) }
    internal val pageSize by lazy { endY + 1 }

    private val endY by lazy {
        val sy = getmaxy(stdscr)
        sy - LOG_LINES_VIEW_BOTTOM_MARGIN
    }
    private val lastPageSize by lazy { pageSize - 1 }

    override suspend fun start() {
        pad = newpad(MAX_LOG_LINES, endX)!!

        scrollok(pad, true)
    }

    override suspend fun stop() {
        delwin(pad)
    }

    /**
     * A solution instead of per-line page up / page down movements would be to use 'clearok(pad,
     * true)' marking the pad as ready for refresh. However, this solution for some reason makes the
     * Status View blink, and that is not acceptable. Hence, retreating for the per-line movement
     * workaround.
     *
     * Leaving for reference: firstVisibleLine -= num
     *
     *         clearok(pad, true)
     *         refresh()
     *         clearok(pad, false)
     */
    fun pageUp() {
        val num = min(pageSize, firstVisibleLine)

        repeat(num) {
            firstVisibleLine--
            refresh()
        }
    }

    fun pageDown() {
        val num = min(pageSize, linesCount - firstVisibleLine)

        repeat(num) {
            firstVisibleLine++
            refresh()
        }
    }

    fun lineUp() {
        if (firstVisibleLine == 0) return

        curs_set(0)
        firstVisibleLine--
        refresh()
    }

    fun lineDown(count: Int) {
        if (firstVisibleLine >= linesCount) return

        curs_set(0)
        firstVisibleLine += count
        refresh()
    }

    fun home() {
        if (firstVisibleLine == 0) return

        val num = min(pageSize, firstVisibleLine)

        firstVisibleLine = num
        pageUp()
    }

    fun end() {
        if (linesCount <= pageSize) {
            return
        }

        firstVisibleLine = linesCount - lastPageSize
        refresh()

        Logger.d("End, FVL: $firstVisibleLine, page size: $pageSize")
    }

    fun clear() {
        wclear(pad)
        linesCount = 0
        firstVisibleLine = 0

        refresh()
    }

    internal fun refresh() {
        val notSeeingLastLine = firstVisibleLine <= linesCount - pageSize

        if (state.isCursorHeld) {
            curs_set(0)
        }

        // call doupdate with pnoutrefresh
        prefresh(pad, firstVisibleLine, 0, 0, 0, endY, endX)

        when {
            state.isCursorHeld -> {
                val y = state.cursorReturnLocation!!.second
                val x = state.cursorReturnLocation!!.first

                wmove(stdscr, y, x)
                curs_set(1)
                wrefresh(stdscr)
            }
            !notSeeingLastLine -> {
                curs_set(1)
            }
            else -> {
                curs_set(0)
            }
        }
    }

    internal fun incrementLinesCount(count: Int = 1) {
        linesCount += count

        if (linesCount >= MAX_LOG_LINES) {
            state = state.copy(overscroll = true)

            linesCount = MAX_LOG_LINES - 1
        }
    }
}
