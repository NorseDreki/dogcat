package ui.status

import dogcat.state.AppliedFilters
import dogcat.LogFilter.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import logger.Logger
import logger.context
import ncurses.*
import ui.ViewPosition

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

    suspend fun inputFilter(): String = memScoped {
        val sx = getmaxx(stdscr)

        val bytePtr = allocArray<ByteVar>(200)
        echo()
        mvwprintw(window, 1, 0, "Enter filter: ")
        //yield()

        withContext(Dispatchers.IO) {
            //wgetch(window)
            wgetnstr(window, bytePtr, 200)
            //readLine() ?: "zzzz"
        }

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
        mvwprintw(window, 0, 50, "Autoscroll ${autoscroll}")
        wattroff(window, COLOR_PAIR(12))
        wrefresh(window)
    }

    fun updateDevice(device: String?, running: Boolean) {
        val cp = if (running) 2 else 1
        wattron(window, COLOR_PAIR(cp))
        mvwprintw(window, 1, 70, device)
        wattroff(window, COLOR_PAIR(cp))
        wrefresh(window)
    }

    fun updatePackageName(packageName: String) {
        mvwprintw(window, 0, 80, "${packageName}")
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
                    mvwprintw(window, 0, 0, "Filter by: ${(it.value as Substring).substring}")
                }

                MinLogLevel::class -> {
                    mvwprintw(window, 0, 30, "${(it.value as MinLogLevel).logLevel} and up")
                }

                ByPackage::class -> {
                    mvwprintw(window, 0, 80, "${(it.value as ByPackage).packageName}")
                }
            }
        }
        wattroff(window, COLOR_PAIR(12))
        wrefresh(window)

        yield()
    }
}
