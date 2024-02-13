package ui.status

import dogcat.state.AppliedFilters
import dogcat.LogFilter.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import logger.Logger
import logger.context
import ncurses.*
import ui.ViewPosition
import kotlin.coroutines.coroutineContext

data class ViewState(
    val filters: AppliedFilters,
    val emulator: String,
    val autoscroll: Boolean
)

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
class StatusView {

    private lateinit var window: CPointer<WINDOW>

    suspend fun start() {
        val sx = getmaxx(stdscr)
        val sy = getmaxy(stdscr)

        //val position = ViewPosition(0, sy - 2, sx, sy - 1)

        window = newwin(0, 0, sy - 2, 0)!!
    }

    suspend fun stop() {
        delwin(window)
    }

    private var filterLength = "Filter: ".length

    suspend fun inputFilter(): String = memScoped {
        val sx = getmaxx(stdscr)

        val bytePtr = allocArray<ByteVar>(200)
        echo()

        leaveok(window, true);

        //mvwprintw(window, 1, filterLength, "")
        // Print the prompt


        val prompt = "Filter: zzz"
        mvwprintw(window, 1, 0, prompt)
        wmove(window, 1, prompt.length)

        val x = getcurx(stdscr)
        val y = getcury(stdscr)

        Logger.d("($x, ${getbegy(window)}}")

        val j = CoroutineScope(coroutineContext).launch {
            while (isActive) {
                delay(10)
                wmove(stdscr, 49 , 0)
                wrefresh(stdscr)
                //Logger.d("moved (${getcurx(window)}, ${getcury(window)})")
            }
        }

        withContext(Dispatchers.IO) {
            //wgetch(window)
            wgetnstr(window, bytePtr, 200)
            //readLine() ?: "zzzz"
        }

        j.cancel()

        Logger.d("????????????????????? ${bytePtr.toKString()}")

        noecho()
        wmove(window, 1, 0)
        waddstr(window, " ".repeat(sx))
        //clrtoeol()
        wrefresh(window)

        return bytePtr.toKString()
    }

    fun updateAutoscroll(autoscroll: Boolean) {
        wattron(window, COLOR_PAIR(12))
        mvwprintw(window, 0, 10, "Autoscroll ${autoscroll}")
        wattroff(window, COLOR_PAIR(12))
        wrefresh(window)
    }

    fun updateDevice(device: String?, running: Boolean) {
        device?.let {
            val cp = if (running) 2 else 1
            wattron(window, COLOR_PAIR(cp))
            mvwprintw(window, 1, getmaxx(window) - device.length, device)
            wattroff(window, COLOR_PAIR(cp))
            wrefresh(window)
        }
    }

    fun updatePackageName(packageName: String) {
        wattron(window, COLOR_PAIR(12))
        mvwprintw(window, 0, getmaxx(window) - packageName.length, packageName)
        wattroff(window, COLOR_PAIR(12))
        wrefresh(window)
    }

    suspend fun updateFilters(filters: AppliedFilters) {
        val sx = getmaxx(stdscr)

        Logger.d("${context()} Preparing to draw applied filters: $filters")
        wmove(window, 0, 0)
        wattron(window, COLOR_PAIR(12))

        waddstr(window, " ".repeat(sx))
        //wclrtoeol(fp)

        filters.forEach {
            when (it.key) {
                Substring::class -> {
                    val fs = "Filter: ${(it.value as Substring).substring}"
                    filterLength = fs.length

                    wattroff(window, COLOR_PAIR(12))
                    mvwprintw(window, 1, 0, fs)
                    wattron(window, COLOR_PAIR(12))
                }

                MinLogLevel::class -> {
                    mvwprintw(window, 0, 0, "${(it.value as MinLogLevel).logLevel} and up")
                }

                ByPackage::class -> {
                    val packageName = (it.value as ByPackage).packageName

                    mvwprintw(window, 0, getmaxx(window) - packageName.length, packageName)
                }
            }
        }
        wattroff(window, COLOR_PAIR(12))
        wrefresh(window)

        yield()
    }
}
