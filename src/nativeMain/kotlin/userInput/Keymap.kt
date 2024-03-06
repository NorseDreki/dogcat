package userInput

import userInput.Keymap.Actions.*
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*

object Keymap {

    @OptIn(ExperimentalForeignApi::class)
    val bindings = mapOf(
        'p'.code to AUTOSCROLL,
        'q'.code to QUIT,

        'f'.code to INPUT_FILTER_BY_SUBSTRING,

        'a'.code to HOME,
        KEY_HOME to HOME,
        'z'.code to END,
        KEY_END to END,
        'w'.code to LINE_UP,
        KEY_UP to LINE_UP,
        's'.code to LINE_DOWN,
        KEY_DOWN to LINE_DOWN,
        'd'.code to PAGE_DOWN,
        KEY_NPAGE to PAGE_DOWN,
        'e'.code to PAGE_UP,
        KEY_PPAGE to PAGE_UP,

        'c'.code to CLEAR_LOGS,

        '6'.code to TOGGLE_FILTER_BY_PACKAGE,
        '7'.code to RESET_FILTER_BY_SUBSTRING,
        '8'.code to RESET_FILTER_BY_MIN_LOG_LEVEL,

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
