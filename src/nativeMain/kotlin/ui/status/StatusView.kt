package ui.status

import AppConfig
import dogcat.LogFilter.*
import dogcat.state.AppliedFilters
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import logger.Logger
import ncurses.*
import kotlin.properties.Delegates

@OptIn(ExperimentalForeignApi::class)
class StatusView {

    data class State(
        val filters: AppliedFilters = mapOf(),
        val packageName: String = "",
        val emulator: String? = null,
        val running: Boolean = false,
        val autoscroll: Boolean = false,
        val isCursorHeld: Boolean = false,
        val cursorReturnLocation: Pair<Int, Int>? = null
    )

    var state: State by Delegates.observable(State()) { p, o, n ->
        updateView(n)
    }

    private lateinit var window: CPointer<WINDOW>

    suspend fun start() {
        val sy = getmaxy(stdscr)

        window = newwin(0, 0, sy - 2, 0)!!
    }

    suspend fun stop() {
        delwin(window)
    }

    private fun updateView(n: State) {
        Logger.d("UPDATE VIEW: $n")

        val sx = getmaxx(stdscr)
        wmove(window, 0, 0)
        wattron(window, COLOR_PAIR(12))
        waddstr(window, " ".repeat(sx))
        wattroff(window, COLOR_PAIR(12))

        mvwprintw(window, 1, 0, AppConfig.INPUT_FILTER_PREFIX)

        updatePackageName(n.packageName)

        n.filters.forEach {
            when (it.key) {
                Substring::class -> {
                    val fs = "${(it.value as Substring).substring}"
                    //filterLength = fs.length

                    if (!state.isCursorHeld) {
                        wattroff(window, COLOR_PAIR(12))
                        //wmove(window, 1, 20)
                        //waddstr(window, fs)
                        mvwprintw(window, 1, AppConfig.INPUT_FILTER_PREFIX.length, fs)
                        wclrtoeol(window)
                        wattron(window, COLOR_PAIR(12))
                    }
                }

                MinLogLevel::class -> {
                    wattron(window, COLOR_PAIR(12))
                    mvwprintw(window, 0, 0, " Log: ${(it.value as MinLogLevel).logLevel.readable.uppercase()}")
                    wattroff(window, COLOR_PAIR(12))
                }

                ByPackage::class -> {
                    val packageName = (it.value as ByPackage).packageName
                    updatePackageName(packageName)
                }
            }
        }
        updateAutoscroll(n.autoscroll)
        updateDevice(n.emulator, n.running)

        wrefresh(window)

        if (state.isCursorHeld) {
            wmove(stdscr, state.cursorReturnLocation!!.second, state.cursorReturnLocation!!.first)
            curs_set(1)
            wrefresh(stdscr)
        }
    }

    //return cursor on input mode!
    private fun updateAutoscroll(autoscroll: Boolean) {
        wattron(window, COLOR_PAIR(12))

        val a = if (autoscroll) "|  Autoscroll" else "|  No autoscroll"
        mvwprintw(window, 0, 15, a)

        wattroff(window, COLOR_PAIR(12))
        //wrefresh(window)
    }

    //return cursor on input mode!
    private fun updateDevice(device: String?, running: Boolean) {
        device?.let {
            curs_set(0)

            val cp = if (running) 2 else 1
            wattron(window, COLOR_PAIR(cp))
            mvwprintw(window, 1, getmaxx(window) - device.length -1, device)
            wattroff(window, COLOR_PAIR(cp))
            //wnoutrefresh(window)
            //wrefresh(window)
        }
    }

    //return cursor on input mode!
    private fun updatePackageName(packageName: String) {
        wattron(window, COLOR_PAIR(12))
        mvwprintw(window, 0, getmaxx(window) - packageName.length - 1, packageName)
        wattroff(window, COLOR_PAIR(12))
        //wrefresh(window)
    }

    /*suspend fun updateFilters(filters: AppliedFilters) {
        val sx = getmaxx(stdscr)

        Logger.d("${context()} Preparing to draw applied filters: $filters")
        wmove(window, 0, 0)
        wattron(window, COLOR_PAIR(12))

        waddstr(window, " ".repeat(sx))
        //wclrtoeol(fp)

        filters.forEach {
            when (it.key) {
                Substring::class -> {
                    val fs = "${(it.value as Substring).substring}"
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

                    updatePackageName(packageName)
                    //mvwprintw(window, 0, getmaxx(window) - packageName.length, packageName)
                }
            }
        }
        wattroff(window, COLOR_PAIR(12))
        wrefresh(window)

        yield()
    }*/
}
