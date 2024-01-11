import Keymap.Actions.*
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*

object Keymap {

    @OptIn(ExperimentalForeignApi::class)
    val bindings = mapOf(
        'p'.code to Autoscroll,
        'q'.code to Quit,

        //add escape to cancel filter
        'f'.code to InputFilterBySubstring,

        'a'.code to Home,
        KEY_HOME to Home,
        'z'.code to End,
        KEY_END to End,
        'w'.code to LineUp,
        KEY_UP to LineUp,
        's'.code to LineDown,
        KEY_DOWN to LineDown,
        'd'.code to PageDown,
        KEY_NPAGE to PageDown,
        'e'.code to PageUp,
        KEY_PPAGE to PageUp,

        'c'.code to ClearLogs,

        '3'.code to ToggleFilterByPackage,
        '4'.code to ResetFilterBySubstring,
        '5'.code to ResetFilterByMinLogLevel,

        '6'.code to MinLogLevelV,
        '7'.code to MinLogLevelD,
        '8'.code to MinLogLevelI,
        '9'.code to MinLogLevelW,
        '0'.code to MinLogLevelE,
    )

    enum class Actions {
        Autoscroll,
        ClearLogs,
        InputFilterBySubstring,

        Home,
        End,
        PageUp,
        PageDown,
        LineUp,
        LineDown,

        ToggleFilterByPackage,
        ResetFilterBySubstring,
        ResetFilterByMinLogLevel,

        MinLogLevelV,
        MinLogLevelD,
        MinLogLevelI,
        MinLogLevelW,
        MinLogLevelE,

        Quit
    }
}
