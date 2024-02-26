package ui.status

import AppConfig
import dogcat.LogFilter.*
import dogcat.state.AppliedFilters
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*
import ui.CommonColors.*
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
        wattron(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
        waddstr(window, " ".repeat(sx))
        wattroff(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
    }

    private fun updateFilters(filters: AppliedFilters) {
        filters.forEach {
            when (it.key) {
                Substring::class -> {
                    val fs = (it.value as Substring).substring

                    if (!state.isCursorHeld) {
                        wattroff(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
                        mvwprintw(window, 1, AppConfig.INPUT_FILTER_PREFIX.length, fs)

                        wclrtoeol(window)
                        wattron(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
                    }
                }

                MinLogLevel::class -> {
                    wattron(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
                    mvwprintw(window, 0, 0, " Log: ${(it.value as MinLogLevel).logLevel.readable.uppercase()}")
                    wattroff(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
                }

                ByPackage::class -> {
                    val packageName = (it.value as ByPackage).packageName
                    updatePackageName(packageName)
                }
            }
        }
    }

    private fun updateAutoscroll(autoscroll: Boolean) {
        wattron(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))

        //extract strings
        val a = if (autoscroll) "|  Autoscroll" else "|  No autoscroll"
        mvwprintw(window, 0, 15, a)

        wattroff(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
    }

    private fun updateDevice(device: String?, running: Boolean) {
        device?.let {
            curs_set(0)

            val colorPairCode =
                if (running) BLACK_ON_WHITE.colorPairCode
                else RED_ON_WHITE.colorPairCode

            wattron(window, COLOR_PAIR(colorPairCode))
            if (!running) {
                wattron(window, A_BOLD.toInt())
            }

            mvwprintw(window, 0, getmaxx(window) - device.length - 1, device)
            wattroff(window, COLOR_PAIR(colorPairCode))

            if (!running) {
                wattroff(window, A_BOLD.toInt())
            }
        }
    }

    private fun updatePackageName(packageName: String) {
        wattron(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
        val s = if (packageName.isNotEmpty()) "$packageName  |  " else "All apps  |  "
        mvwprintw(window, 0, getmaxx(window) - s.length - 1 - 15, s)
        wattroff(window, COLOR_PAIR(BLACK_ON_WHITE.colorPairCode))
    }
}
