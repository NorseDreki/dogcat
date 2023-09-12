import dogcat.LogcatState.CapturingInput
import dogcat.ClearLogs
import dogcat.Filter
import dogcat.StopEverything
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import ncurses.*
import platform.posix.exit

@OptIn(ExperimentalForeignApi::class)
class Keymap(
    val memScope: MemScope,
    val pad: Pad
) {
    @OptIn(ExperimentalForeignApi::class)
    suspend fun processInputKey(
        key: Int
    ) {
        var lastIndex = 0

        when (key) {
            'f'.code -> {
                mvwprintw(stdscr, 0, 0, ":")
                clrtoeol()
                echo()

                val bytePtr = memScope.allocArray<ByteVar>(200)
                getnstr(bytePtr, 200)
                noecho()
                pad.clear()

                dogcat(Filter.ByString(bytePtr.toKString()))
            }

            'q'.code -> {
                dogcat(StopEverything)
                pad.terminate()
                exit(0)
            }

            'a'.code -> pad.home()

            'z'.code -> pad.end()

            'w'.code -> pad.lineUp()

            's'.code -> pad.lineDown()

            'd'.code -> pad.pageDown()

            'e'.code -> pad.pageUp()

            '6'.code -> {
                dogcat(Filter.ToggleLogLevel("V"))
            }

            '7'.code -> {
                dogcat(Filter.ToggleLogLevel("D"))
            }

            '8'.code -> {
                dogcat(Filter.ToggleLogLevel("I"))
            }

            '9'.code -> {
                dogcat(Filter.ToggleLogLevel("W"))
            }

            '0'.code -> {
                dogcat(Filter.ToggleLogLevel("E"))
            }

            'c'.code -> {
                dogcat(ClearLogs)
            }

            'o'.code -> {
                val indices = mutableListOf<Int>()
                (dogcat.state.value as CapturingInput).problems.map { println("aaaa $it");it.index }.take(1).toList(indices)

                pad.toLine(indices[lastIndex])
                lastIndex += 1
            }
        }
    }

    enum class Keys {
        Q,
        ECHAP,
        ENTER,
        SPACE,
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    val keyMap = mapOf(
        'q'.toInt() to Keys.Q,
        27 to Keys.ECHAP,
        10 to Keys.ENTER,
        32 to Keys.SPACE,
        259 to Keys.UP,
        258 to Keys.DOWN,
        260 to Keys.LEFT,
        261 to Keys.RIGHT
    )
}
