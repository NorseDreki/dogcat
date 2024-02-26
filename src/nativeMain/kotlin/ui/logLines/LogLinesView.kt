package ui.logLines

import AppConfig.LOG_LINES_VIEW_BOTTOM_MARGIN
import dogcat.DogcatConfig.MAX_LOG_LINES
import kotlinx.cinterop.ExperimentalForeignApi
import logger.Logger
import logger.context
import ncurses.*
import ui.HasLifecycle
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
class LogLinesView : HasLifecycle {

    internal data class ViewPosition(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int,
    )

    data class State(
        val autoscroll: Boolean = false,
        val isCursorHeld: Boolean = false,
        val cursorReturnLocation: Pair<Int, Int>? = null,
    )

    var state = State()

    internal val sx = getmaxx(stdscr)
    private val sy = getmaxy(stdscr)

    internal val position = ViewPosition(0, 0, sx, sy - LOG_LINES_VIEW_BOTTOM_MARGIN)

    internal val pad = newpad(MAX_LOG_LINES, position.endX)
    internal val pageSize = position.endY - position.startY + 1

    // '-1' in order to leave bottom line to cursor
    private val lastPageSize = pageSize - 1

    init {
        scrollok(pad, true)
    }

    private var firstVisibleLine = 0
    internal var linesCount = 0

    override suspend fun start() {
        TODO("Not yet implemented")
    }

    override suspend fun stop() {
        delwin(pad)
    }

    /**
     * A solution instead of per-line page up / page down movements would be to use 'clearok(pad, true)' marking
     * the pad as ready for refresh. However, this solution for some reason make the Status View blink, and that is
     * not acceptable. Hence, retreating for the per-line movement workaround.
     *
     * Leaving for reference:
     *         firstVisibleLine -= num
     *
     *         clearok(pad, true)
     *         refresh()
     *         clearok(pad, false)
     */

    fun pageUp() {
        val num = min(pageSize, firstVisibleLine)

        repeat(num) {
            firstVisibleLine--
            refresh()
        }

        Logger.d("Page Up by $pageSize, $firstVisibleLine")
    }

    fun pageDown() {
        //maybe also hide cursor? sometimes it appears at very bottom

        val num = min(pageSize, linesCount - firstVisibleLine)

        repeat(num) {
            firstVisibleLine++
            refresh()
        }

        Logger.d("Page Down by $pageSize, $firstVisibleLine")
    }

    fun lineUp() {
        if (firstVisibleLine == 0) return

        curs_set(0)
        firstVisibleLine--
        refresh()
    }

    fun lineDown(count: Int) {
        if (firstVisibleLine == linesCount) return

        curs_set(0)
        firstVisibleLine += count
        refresh()
    }

    fun home() {
        if (firstVisibleLine == 0) return

        val num = min(pageSize, firstVisibleLine)

        firstVisibleLine = num
        pageUp()
    }

    fun end() {
        //to few lines for page down, we are at beggining of log
        if (linesCount <= pageSize) {
            return
        }

        firstVisibleLine = linesCount - lastPageSize
        refresh()
        Logger.d("End $firstVisibleLine")
    }

    suspend fun clear() {
        wclear(pad)
        linesCount = 0
        firstVisibleLine = 0

        Logger.d("${context()} Cleared pad")
        refresh()
    }

    //draw fake cursor
    internal fun refresh() {
        val notSeeingLastLine = firstVisibleLine <= linesCount - pageSize

        if (state.isCursorHeld) {
            curs_set(0)
        }

        prefresh(pad, firstVisibleLine, 0, position.startY, position.startX, position.endY, position.endX)
        //call doupdate with pnoutrefresh

        when {
            state.isCursorHeld -> {
                wmove(stdscr, state.cursorReturnLocation!!.second, state.cursorReturnLocation!!.first)
                curs_set(1)
                wrefresh(stdscr)
            }

            !notSeeingLastLine -> {
                curs_set(1)
            }

            else -> {
                curs_set(0)
            }
        }
    }

    internal fun recordLine(count: Int = 1) {
        linesCount += count

        if (linesCount >= MAX_LOG_LINES) {
            linesCount = MAX_LOG_LINES - 1
        }
    }
}
