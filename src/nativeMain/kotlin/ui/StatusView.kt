package ui

import dogcat.AppliedFilters
import dogcat.LogFilter
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.yield
import ncurses.*

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
class StatusView {

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)

    val position = PadPosition(0, sy - 2, sx, sy - 1)

    val fp = newwin(0, 0, position.startY,0)


    suspend fun printStatusLine(it: AppliedFilters) {
        val sx = getmaxx(stdscr)

        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Preparing to draw applied filters: $it")
        wmove(fp, 0, 0)
        wattron(fp, COLOR_PAIR(12))

        waddstr(fp, " ".repeat(sx))

        //wclrtoeol(fp)

        it.forEach {
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
