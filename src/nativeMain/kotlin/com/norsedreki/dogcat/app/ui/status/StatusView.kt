/*
 * SPDX-FileCopyrightText: Copyright (C) 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.status

import com.norsedreki.dogcat.LogFilter.ByPackage
import com.norsedreki.dogcat.LogFilter.MinLogLevel
import com.norsedreki.dogcat.LogFilter.Substring
import com.norsedreki.dogcat.app.AppConfig.STATUS_VIEW_AUTOSCROLL_LEFT_MARGIN
import com.norsedreki.dogcat.app.AppConfig.STATUS_VIEW_BOTTOM_MARGIN
import com.norsedreki.dogcat.app.ui.CommonColors.BLACK_ON_WHITE
import com.norsedreki.dogcat.app.ui.CommonColors.RED_ON_WHITE
import com.norsedreki.dogcat.app.ui.HasLifecycle
import com.norsedreki.dogcat.app.ui.Strings.ALL_APPS
import com.norsedreki.dogcat.app.ui.Strings.AUTOSCROLL
import com.norsedreki.dogcat.app.ui.Strings.INPUT_FILTER_PREFIX
import com.norsedreki.dogcat.app.ui.Strings.LOG_LEVEL_PREFIX
import com.norsedreki.dogcat.app.ui.Strings.NO_AUTOSCROLL
import com.norsedreki.dogcat.app.ui.Strings.SEPARATOR
import com.norsedreki.dogcat.state.LogFilters
import kotlin.properties.Delegates
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.A_BOLD
import ncurses.COLOR_PAIR
import ncurses.WINDOW
import ncurses.curs_set
import ncurses.delwin
import ncurses.getmaxx
import ncurses.getmaxy
import ncurses.mvwprintw
import ncurses.newwin
import ncurses.stdscr
import ncurses.waddstr
import ncurses.wattroff
import ncurses.wattron
import ncurses.wclrtoeol
import ncurses.wmove
import ncurses.wrefresh

@OptIn(ExperimentalForeignApi::class)
class StatusView : HasLifecycle {

    data class State(
        val filters: LogFilters = mapOf(),
        val packageName: String = "",
        val deviceLabel: String = "",
        val isDeviceOnline: Boolean = false,
        val autoscroll: Boolean = false,
        val isCursorHeld: Boolean = false,
        val cursorReturnLocation: Pair<Int, Int>? = null
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
        updateBackground()
        updateDevice(state.deviceLabel, state.isDeviceOnline)
        updatePackageName(state.packageName)
        updateFilters(state.filters)
        updateAutoscroll(state.autoscroll)

        wrefresh(window)

        if (state.isCursorHeld) {
            val x = state.cursorReturnLocation!!.first
            val y = state.cursorReturnLocation.second

            wmove(stdscr, y, x)
            curs_set(1)
            wrefresh(stdscr)
        }
    }

    private fun updateBackground() {
        val sx = getmaxx(stdscr)
        wmove(window, 0, 0)

        wattron(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
        waddstr(window, " ".repeat(sx))
        wattroff(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
    }

    private fun updateFilters(filters: LogFilters) {
        filters.forEach {
            when (it.key) {
                Substring::class -> {
                    val s = (it.value as Substring).substring

                    if (!state.isCursorHeld) {
                        wattroff(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
                        mvwprintw(window, 1, INPUT_FILTER_PREFIX.length, s)

                        wclrtoeol(window)
                        wattron(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
                    }
                }
                MinLogLevel::class -> {
                    wattron(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))

                    val logLevelAllCap = (it.value as MinLogLevel).logLevel.label.uppercase()

                    val s = "$LOG_LEVEL_PREFIX$logLevelAllCap"
                    mvwprintw(window, 0, 0, s)

                    wattroff(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
                }
                ByPackage::class -> {
                    val packageName = (it.value as ByPackage).packageName
                    updatePackageName(packageName)
                }
            }
        }
    }

    private fun updateAutoscroll(autoscroll: Boolean) {
        wattron(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))

        val s = if (autoscroll) AUTOSCROLL else NO_AUTOSCROLL
        mvwprintw(window, 0, STATUS_VIEW_AUTOSCROLL_LEFT_MARGIN, s)

        wattroff(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
    }

    private fun updateDevice(device: String, running: Boolean) {
        curs_set(0)

        val colorPairCode =
            if (running) {
                BLACK_ON_WHITE.colorPairCode
            } else {
                RED_ON_WHITE.colorPairCode
            }

        wattron(window, COLOR_PAIR(colorPairCode))
        if (!running) {
            wattron(window, A_BOLD.toInt())
        }

        mvwprintw(window, 0, getmaxx(window) - device.length - 1, device)
        wattroff(window, COLOR_PAIR(colorPairCode))

        if (!running) {
            wattroff(window, A_BOLD.toInt())
        }
    }

    private fun updatePackageName(packageName: String) {
        wattron(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))

        val packageLabel =
            if (packageName.isNotEmpty()) {
                "$packageName$SEPARATOR"
            } else {
                "$ALL_APPS$SEPARATOR"
            }

        val x = getmaxx(window) - packageLabel.length - 1 - state.deviceLabel.length
        mvwprintw(window, 0, x, packageLabel)

        wattroff(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
    }
}
