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

    var state: State by Delegates.observable(State()) { _, _, newValue ->
        updateView(newValue)
    }

    private lateinit var window: CPointer<WINDOW>

    fun start() {
        val sy = getmaxy(stdscr)
        window = newwin(0, 0, sy - 2, 0)!!
    }

    fun stop() {
        delwin(window)
    }

    private fun updateView(n: State) {
        //Logger.d("UPDATE VIEW: $n")

        updateBackground()
        updateDevice(n.emulator, n.running)
        updatePackageName(n.packageName)
        updateFilters(n.filters)
        updateAutoscroll(n.autoscroll)

        wrefresh(window)

        //do not return if alreadey in place
        if (state.isCursorHeld) {
            //Logger.d("STATUS VIEW -- RETURN CURSOR")
            wmove(stdscr, state.cursorReturnLocation!!.second, state.cursorReturnLocation!!.first)
            curs_set(1)
            wrefresh(stdscr)
        }
    }

    private fun updateBackground() {
        val sx = getmaxx(stdscr)
        wmove(window, 0, 0)
        wattron(window, COLOR_PAIR(12))
        waddstr(window, " ".repeat(sx))
        wattroff(window, COLOR_PAIR(12))
    }

    private fun updateFilters(filters: AppliedFilters) {
        filters.forEach {
            when (it.key) {
                Substring::class -> {
                    val fs = (it.value as Substring).substring

                    if (!state.isCursorHeld) {
                        wattroff(window, COLOR_PAIR(12))
                        mvwprintw(window, 1, AppConfig.INPUT_FILTER_PREFIX.length, fs)

                        //TODO don't clear to end, there is device info
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
    }

    private fun updateAutoscroll(autoscroll: Boolean) {
        wattron(window, COLOR_PAIR(12))

        //extract strings
        val a = if (autoscroll) "|  Autoscroll" else "|  No autoscroll"
        mvwprintw(window, 0, 15, a)

        wattroff(window, COLOR_PAIR(12))
    }

    private fun updateDevice(device: String?, running: Boolean) {
        device?.let {
            curs_set(0)

            val cp = if (running) 2 else 1

            wattron(window, COLOR_PAIR(cp))
            mvwprintw(window, 0, getmaxx(window) - device.length - 1, device)
            wattroff(window, COLOR_PAIR(cp))
        }
    }

    private fun updatePackageName(packageName: String) {
        wattron(window, COLOR_PAIR(12))
        val s = if (packageName.isNotEmpty()) "$packageName on " else "All apps on "
        mvwprintw(window, 0, getmaxx(window) - s.length - 1 - 15, s)
        wattroff(window, COLOR_PAIR(12))
    }
}
