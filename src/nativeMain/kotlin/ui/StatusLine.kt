package ui

import Logger
import ServiceLocator
import dogcat.AppliedFilters
import dogcat.LogFilter.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import ncurses.*
import ui.logLines.LogLinesView

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
suspend fun LogLinesView.printStatusLine(it: AppliedFilters) {
    val sx = getmaxx(stdscr)

    Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Preparing to draw applied filters: $it")
    wmove(fp, 0, 0)
    wattron(fp, COLOR_PAIR(12))

    waddstr(fp, " ".repeat(sx))

    //wclrtoeol(fp)

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
    //prefresh(fp, 0, 0, 0, 0, 2, sx)
   // prefresh(fp, 0, 0, position.startY, position.startX, position.endY, position.endX);


    withContext(Dispatchers.IO) {
        launch {
            ServiceLocator.appStateFlow.state.collectLatest {
            //    withContext(Dispatchers.Main) {
                    mvwprintw(fp, 0, 70, "Autoscroll ${it.autoscroll}")
                    wattroff(fp, COLOR_PAIR(12))
                    wrefresh(fp)
                    //refresh()
                    yield()
            //    }
            }
        }
    }
    /*ServiceLocator.appStateFlow.state.collect() {

    }*/

    wattroff(fp, COLOR_PAIR(12))
    wrefresh(fp)
    //refresh()
    yield()
}
