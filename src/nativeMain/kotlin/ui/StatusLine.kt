package ui

import Logger
import dogcat.AppliedFilters
import dogcat.LogFilter.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.yield
import ncurses.*

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
suspend fun Pad.printStatusLine(it: AppliedFilters) {
    val sx = getmaxx(stdscr)

    Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Preparing to draw applied filters: $it")
    wmove(fp, 0, 0)
    wattron(fp, COLOR_PAIR(12))
    wclrtoeol(fp)

    it.forEach {
        when (it.key) {
            Substring::class -> {
                mvwprintw(fp, 0, 0, "Filter by: ${(it.value as Substring).substring}")
            }
            MinLogLevel::class -> {
                mvwprintw(fp, 0, 30, "${(it.value as MinLogLevel).logLevel} and up")
            }
            ByPackage::class -> {
                mvwprintw(fp, 0, 80, "${(it.value as ByPackage).packageName} on")
            }
        }
    }
    prefresh(fp, 0, 0, 0, 0, 2, sx)

    wattroff(fp, COLOR_PAIR(12))
    refresh()
    yield()
}
