/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app

import com.norsedreki.dogcat.app.Keymap.Actions.AUTOSCROLL
import com.norsedreki.dogcat.app.Keymap.Actions.CLEAR_LOGS
import com.norsedreki.dogcat.app.Keymap.Actions.END
import com.norsedreki.dogcat.app.Keymap.Actions.HELP
import com.norsedreki.dogcat.app.Keymap.Actions.HOME
import com.norsedreki.dogcat.app.Keymap.Actions.INPUT_FILTER_BY_SUBSTRING
import com.norsedreki.dogcat.app.Keymap.Actions.LINE_DOWN
import com.norsedreki.dogcat.app.Keymap.Actions.LINE_UP
import com.norsedreki.dogcat.app.Keymap.Actions.MIN_LOG_LEVEL_D
import com.norsedreki.dogcat.app.Keymap.Actions.MIN_LOG_LEVEL_E
import com.norsedreki.dogcat.app.Keymap.Actions.MIN_LOG_LEVEL_I
import com.norsedreki.dogcat.app.Keymap.Actions.MIN_LOG_LEVEL_V
import com.norsedreki.dogcat.app.Keymap.Actions.MIN_LOG_LEVEL_W
import com.norsedreki.dogcat.app.Keymap.Actions.PAGE_DOWN
import com.norsedreki.dogcat.app.Keymap.Actions.PAGE_UP
import com.norsedreki.dogcat.app.Keymap.Actions.QUIT
import com.norsedreki.dogcat.app.Keymap.Actions.TOGGLE_FILTER_BY_PACKAGE
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.KEY_DOWN
import ncurses.KEY_END
import ncurses.KEY_HOME
import ncurses.KEY_NPAGE
import ncurses.KEY_PPAGE
import ncurses.KEY_UP

@OptIn(ExperimentalForeignApi::class)
object Keymap {

    fun printedShortcut(keyCode: Int) = keyNames[keyCode] ?: keyCode.toChar().toString()

    val bindings =
        mapOf(
            // Navigation actions
            'e'.code to HOME,
            KEY_HOME to HOME,
            'd'.code to END,
            KEY_END to END,
            KEY_UP to LINE_UP,
            KEY_DOWN to LINE_DOWN,
            's'.code to PAGE_DOWN,
            KEY_NPAGE to PAGE_DOWN,
            'w'.code to PAGE_UP,
            KEY_PPAGE to PAGE_UP,

            // Filter actions
            'f'.code to INPUT_FILTER_BY_SUBSTRING,
            't'.code to TOGGLE_FILTER_BY_PACKAGE,
            // '7'.code to RESET_FILTER_BY_SUBSTRING,
            // '8'.code to RESET_FILTER_BY_MIN_LOG_LEVEL,

            // Log level actions
            '1'.code to MIN_LOG_LEVEL_V,
            '2'.code to MIN_LOG_LEVEL_D,
            '3'.code to MIN_LOG_LEVEL_I,
            '4'.code to MIN_LOG_LEVEL_W,
            '5'.code to MIN_LOG_LEVEL_E,

            // Other actions
            '?'.code to HELP,
            'r'.code to AUTOSCROLL,
            'c'.code to CLEAR_LOGS,
            'q'.code to QUIT,
        )

    enum class Actions(val description: String) {
        // Navigation actions
        HOME("Move to the beginning of log lines (Home)"),
        END("Move to the end of log lines (End)"),
        PAGE_UP("Move one screen up (Page Up)"),
        PAGE_DOWN("Move one screen down (Page Down)"),
        LINE_UP("Move one line up"),
        LINE_DOWN("Move one line down"),

        // Filter actions
        INPUT_FILTER_BY_SUBSTRING("Filter log lines by inputted string"),
        TOGGLE_FILTER_BY_PACKAGE("Toggle filtering log lines by app package"),
        RESET_FILTER_BY_SUBSTRING("Reset the filter for log lines by substring"),
        RESET_FILTER_BY_MIN_LOG_LEVEL("Reset the filter for log lines by minimum log level"),

        // Log level actions
        MIN_LOG_LEVEL_V("Set the minimum log level to V"),
        MIN_LOG_LEVEL_D("Set the minimum log level to D"),
        MIN_LOG_LEVEL_I("Set the minimum log level to I"),
        MIN_LOG_LEVEL_W("Set the minimum log level to W"),
        MIN_LOG_LEVEL_E("Set the minimum log level to E"),

        // Other actions
        HELP("Display this window on screen or hide it"),
        AUTOSCROLL("Toggle automatic scrolling of log lines on screen"),
        CLEAR_LOGS("Clear log source and empty log lines on screen"),
        QUIT("Quit this application")
    }

    private val keyNames =
        mapOf(
            KEY_HOME to "Home",
            KEY_END to "End",
            KEY_UP to "Up",
            KEY_DOWN to "Down",
            KEY_NPAGE to "Page Down",
            KEY_PPAGE to "Page Up",
        )
}
