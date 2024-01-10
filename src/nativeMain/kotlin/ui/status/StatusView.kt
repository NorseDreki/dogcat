package ui.status

import dogcat.AppliedFilters
import dogcat.LogFilter
import dogcat.LogFilter.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import ncurses.*
import ui.ViewPosition

data class ViewState(
    val filters: AppliedFilters,
    val emulator: String,
    val autoscroll: Boolean
)


@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
class StatusView {

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)

    val position = ViewPosition(0, sy - 2, sx, sy - 1)

    val fp = newwin(0, 0, position.startY, 0)

    suspend fun stop() {
        delwin(fp)
    }

    suspend fun inputFilter(): String = memScoped {

        val bytePtr = allocArray<ByteVar>(200)
        echo()
        mvwprintw(fp, 1, 0, "Enter filter: ")
        //yield()

        //withContext(Dispatchers.IO) {
        wgetnstr(fp, bytePtr, 200)
        //}

        noecho()
        wmove(fp, 1, 0)
        waddstr(fp, " ".repeat(sx))
        //clrtoeol()
        wrefresh(fp)

        return bytePtr.toKString()
    }

    fun updateAutoscroll(autoscroll: Boolean) {
        wattron(fp, COLOR_PAIR(12))
        mvwprintw(fp, 0, 50, "Autoscroll ${autoscroll}")
        wattroff(fp, COLOR_PAIR(12))
        wrefresh(fp)
    }

    fun updateDevice(device: String?, running: Boolean) {
        val cp = if (running) 2 else 1
        wattron(fp, COLOR_PAIR(cp))
        mvwprintw(fp, 1, 70, device)
        wattroff(fp, COLOR_PAIR(cp))
        wrefresh(fp)
    }

    fun updatePackageName(packageName: String) {
        mvwprintw(fp, 0, 80, "${packageName}")
        wrefresh(fp)
    }

    suspend fun updateFilters(filters: AppliedFilters) {
        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Preparing to draw applied filters: $filters")
        wmove(fp, 0, 0)
        wattron(fp, COLOR_PAIR(12))

        waddstr(fp, " ".repeat(sx))
        //wclrtoeol(fp)

        filters.forEach {
            when (it.key) {
                Substring::class -> {
                    mvwprintw(fp, 0, 0, "Filter by: ${(it.value as Substring).substring}")
                }

                MinLogLevel::class -> {
                    mvwprintw(fp, 0, 30, "${(it.value as MinLogLevel).logLevel} and up")
                }

                ByPackage::class -> {
                    mvwprintw(fp, 0, 80, "${(it.value as ByPackage).packageName}")
                }
            }
        }
        wattroff(fp, COLOR_PAIR(12))
        wrefresh(fp)

        yield()
    }
}
