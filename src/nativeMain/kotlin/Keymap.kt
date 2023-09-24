import dogcat.*
import dogcat.Command.*
import dogcat.LogFilter.*
import dogcat.LogcatState.CapturingInput
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import ncurses.*
import platform.posix.exit

@OptIn(ExperimentalForeignApi::class)
class Keymap(
    val memScope: MemScope,
    val pad: Pad,
    val pad2: Pad,
) {
    @OptIn(ExperimentalForeignApi::class)
    suspend fun processInputKey(
        key: Int
    ) {
        when (key) {
            'f'.code -> {
                //mvwprintw(stdscr, 0, 0, ":")
                //mvwprintw(pad2.fp, 0, 0, "Filter by: ")
                //prefresh(pad2.fp, 0, 0, 0, 0, 2, 100);
                //wclrtoeol(pad2.fp)
                //wclrtoeol()

                //echo()

                val bytePtr = memScope.allocArray<ByteVar>(200)

                wmove(pad2.fp, 0, 0)

                wgetnstr(pad2.fp, bytePtr, 200)
                //noecho()
                //pad.clear()

                dogcat(FilterBy(Substring(bytePtr.toKString())))
            }

            'q'.code -> {
                dogcat(StopEverything)
                pad.terminate()
                pad2.terminate()
                exit(0)
            }

            'a'.code, KEY_HOME -> pad.home()

            'z'.code, KEY_END -> pad.end()

            'w'.code, KEY_UP -> pad.lineUp()

            's'.code, KEY_DOWN -> pad.lineDown()

            'd'.code, KEY_NPAGE -> pad.pageDown()

            'e'.code, KEY_PPAGE -> pad.pageUp()

            '3'.code -> {
                dogcat(ResetFilter(ByPackage::class))
            }
            '4'.code -> {
                dogcat(ResetFilter(Substring::class))
            }
            '5'.code -> {
                dogcat(ResetFilter(MinLogLevel::class))
            }

            '6'.code -> {
                dogcat(FilterBy(MinLogLevel("V")))
            }

            '7'.code -> {
                dogcat(FilterBy(MinLogLevel("D")))
            }

            '8'.code -> {
                dogcat(FilterBy(MinLogLevel("I")))
            }

            '9'.code -> {
                dogcat(FilterBy(MinLogLevel("W")))
            }

            '0'.code -> {
                dogcat(FilterBy(MinLogLevel("E")))
            }

            'c'.code -> {
                dogcat(ClearLogs)
            }

            't'.code -> {
                dogcat(FilterBy(ByPackage("", "")))
            }
        }
    }
}
