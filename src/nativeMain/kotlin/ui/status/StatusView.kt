package ui.status

import dogcat.AppliedFilters
import dogcat.LogFilter
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import ncurses.*
import ui.logLines.PadPosition

data class ViewState(
    val filters: AppliedFilters,
    val emulator: String,
    val autoscroll: Boolean
)


@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
class StatusView {

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)

    val position = PadPosition(0, sy - 2, sx, sy - 1)

    val fp = newwin(0, 0, position.startY,0)

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

        wmove(fp, 1, 0)
        waddstr(fp, " ".repeat(sx))
        //clrtoeol()
        wrefresh(fp)

        return bytePtr.toKString()
    }


    suspend fun updateFilters(filters: AppliedFilters) {
        val sx = getmaxx(stdscr)

        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Preparing to draw applied filters: $filters")
        wmove(fp, 0, 0)
        wattron(fp, COLOR_PAIR(12))

        waddstr(fp, " ".repeat(sx))

        //wclrtoeol(fp)

        filters.forEach {
            when (it.key) {
                LogFilter.Substring::class -> {
                    mvwprintw(fp, 0, 0, "Filter by: ${(it.value as LogFilter.Substring).substring}")
                }
                LogFilter.MinLogLevel::class -> {
                    mvwprintw(fp, 0, 30, "${(it.value as LogFilter.MinLogLevel).logLevel} and up")
                }
                LogFilter.ByPackage::class -> {
                    mvwprintw(fp, 0, 80, "${(it.value as LogFilter.ByPackage).packageName} on")
                }
            }
        }
        //prefresh(fp, 0, 0, 0, 0, 2, sx)
        // prefresh(fp, 0, 0, position.startY, position.startX, position.endY, position.endX);

        wattroff(fp, COLOR_PAIR(12))
        wrefresh(fp)
        //refresh()
        yield()
    }
}
