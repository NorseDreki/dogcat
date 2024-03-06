package com.norsedreki.dogcat.app

import com.norsedreki.dogcat.app.Keymap.Actions.*
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*

object Keymap {

    @OptIn(ExperimentalForeignApi::class)
    val bindings = mapOf(
        'r'.code to AUTOSCROLL,
        'q'.code to QUIT,

        'f'.code to INPUT_FILTER_BY_SUBSTRING,

        'a'.code to HOME,
        KEY_HOME to HOME,
        'd'.code to END,
        KEY_END to END,
        KEY_UP to LINE_UP,
        KEY_DOWN to LINE_DOWN,
        's'.code to PAGE_DOWN,
        KEY_NPAGE to PAGE_DOWN,
        'w'.code to PAGE_UP,
        KEY_PPAGE to PAGE_UP,

        'c'.code to CLEAR_LOGS,

        't'.code to TOGGLE_FILTER_BY_PACKAGE,
        //'7'.code to RESET_FILTER_BY_SUBSTRING,
        //'8'.code to RESET_FILTER_BY_MIN_LOG_LEVEL,

        '1'.code to MIN_LOG_LEVEL_V,
        '2'.code to MIN_LOG_LEVEL_D,
        '3'.code to MIN_LOG_LEVEL_I,
        '4'.code to MIN_LOG_LEVEL_W,
        '5'.code to MIN_LOG_LEVEL_E,
    )

    enum class Actions {
        AUTOSCROLL,
        CLEAR_LOGS,
        INPUT_FILTER_BY_SUBSTRING,

        HOME,
        END,
        PAGE_UP,
        PAGE_DOWN,
        LINE_UP,
        LINE_DOWN,

        TOGGLE_FILTER_BY_PACKAGE,
        RESET_FILTER_BY_SUBSTRING,
        RESET_FILTER_BY_MIN_LOG_LEVEL,

        MIN_LOG_LEVEL_V,
        MIN_LOG_LEVEL_D,
        MIN_LOG_LEVEL_I,
        MIN_LOG_LEVEL_W,
        MIN_LOG_LEVEL_E,

        QUIT
    }
}
