import ServiceLocator.dogcat
import dogcat.Command.*
import dogcat.LogFilter.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import ncurses.*
import platform.posix.exit
import ui.Pad

@OptIn(ExperimentalForeignApi::class)
class Keymap(
    val memScope: MemScope,
    val pad: Pad,
    val pad2: Pad,
    val pkg: String? = null
) {

    var isPackageFilteringEnabled = pkg != null

    @OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
    suspend fun processInputKey(
        key: Int
    ) {
        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Process key $key")

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

                withContext(Dispatchers.IO) {
                    wgetnstr(pad2.fp, bytePtr, 200)
                }
                //noecho()
                //pad.clear()

                dogcat(FilterBy(Substring(bytePtr.toKString())))
            }

            'q'.code -> { // catch control-c
                dogcat(Stop)
                pad.terminate()
                pad2.terminate()
                endwin()
                //resetty()
                exit(0)
            }

            'a'.code, KEY_HOME -> pad.home()

            'z'.code, KEY_END -> pad.end()

            'w'.code, KEY_UP -> pad.lineUp()

            's'.code, KEY_DOWN -> pad.lineDown()

            'd'.code, KEY_NPAGE -> pad.pageDown()

            'e'.code, KEY_PPAGE -> pad.pageUp()

            '3'.code -> {
                isPackageFilteringEnabled =
                    if (isPackageFilteringEnabled) {
                        dogcat(ResetFilter(ByPackage::class))
                        false
                    } else {
                        dogcat(Start.SelectAppByPackage(pkg!!))
                        true
                    }
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
                dogcat(ClearLogSource)
            }
        }
    }
}
