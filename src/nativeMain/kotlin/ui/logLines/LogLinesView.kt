package ui.logLines

import dogcat.DogcatConfig.MAX_LOG_LINES
import kotlinx.cinterop.ExperimentalForeignApi
import logger.Logger
import logger.context
import ncurses.*
import ui.ViewPosition
import kotlin.math.min
import kotlin.properties.Delegates

@OptIn(ExperimentalForeignApi::class)
class LogLinesView {

    data class State(
        val autoscroll: Boolean = false,
        val isCursorHeld: Boolean = false,
        val cursorReturnLocation: Pair<Int, Int>? = null,

        val holdRequest: ((x: Int, y: Int) -> Unit)? = null
    )

    var state: State by Delegates.observable(State()) { p, o, n ->
    }

    //var isCursorHeld: Boolean = false

    private val sx = getmaxx(stdscr)
    private val sy = getmaxy(stdscr)

    internal val position = ViewPosition(0, 0, sx, sy - 4)

    internal val pad = newpad((MAX_LOG_LINES * 1.3).toInt(), position.endX) //fix guesstimation
    internal val pageSize = position.endY - position.startY + 1
    private val lastPageSize = pageSize - 1

    //internal var autoscroll = false

    init {
        scrollok(pad, true)
    }

    private var firstVisibleLine = 0
    internal var linesCount = 0

    fun stop() {
        delwin(pad)
    }

    fun pageUp() {
        //firstVisibleLine -= pageSize
        //refresh()

        val num = min(pageSize, firstVisibleLine)

        /*firstVisibleLine -= num

        clearok(pad, true)
        refresh()
        clearok(pad, false)*/

        repeat(num) {
            firstVisibleLine--
            refresh()
        }

        Logger.d("Page Up by $pageSize, $firstVisibleLine")
    }

    fun pageDown() {
        //firstVisibleLine += pageSize
        //refresh()

        //disable page down for some cases?

        /*val num = if (autoscroll) {
            min(pageSize, linesCount - lastPageSize - firstVisibleLine)
        } else {
            min(pageSize, linesCount - firstVisibleLine)
        }*/

        val num = min(pageSize, linesCount - firstVisibleLine)

        /*firstVisibleLine += num

        clearok(pad, true)
        refresh()
        clearok(pad, false)*/

        //val num = min(pageSize, linesCount - firstVisibleLine)

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
        Logger.d("Up, $firstVisibleLine")
    }

    fun lineDown(second: Int) {
        if (firstVisibleLine == linesCount) return

        curs_set(0)
        firstVisibleLine += second
        refresh()
        Logger.d("Down, $firstVisibleLine")
    }

    fun home() {
        /*firstVisibleLine = 0

        clearok(pad, true)
        refresh()
        clearok(pad, false)*/


        //firstVisibleLine = 0
        //refresh()
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

        //prefresh(pad, linesCount, 0, position.startY, position.startX, position.endY, position.endX)

        firstVisibleLine = linesCount - lastPageSize
        refresh()

        //refresh()

        Logger.d("End $firstVisibleLine")
    }

    suspend fun clear() {
        //prefresh(pad, linesCount, 0, position.startY, position.startX, position.endY, position.endX)

        wclear(pad)
        linesCount = 0
        firstVisibleLine = 0

        Logger.d("${context()} Cleared pad")
        //werase(fp)
        // refresh()
        //wmove(pad, 0, 0)

        //wprintw(pad, "123\n")
        //curs_set(1)
        refresh()
    }


    //draw fake cursor
    internal fun refresh() {
        val notSeeingLastLine = firstVisibleLine <= linesCount - pageSize

        prefresh(pad, firstVisibleLine, 0, position.startY, position.startX, position.endY, position.endX)
        //call doupdate with pnoutrefresh

        when {
            state.isCursorHeld -> {
                Logger.d("Cursor held")
               // wmove(stdscr, state.cursorReturnLocation!!.second, state.cursorReturnLocation!!.first)
                curs_set(1)
                wrefresh(stdscr)
            }

            !notSeeingLastLine -> {
                val x = getcurx(pad)
                val y = getpary(pad)
                Logger.d("Hold location $x $y")
                state.holdRequest!!.invoke(x, y)
                curs_set(1)
            }

            else -> {
                curs_set(0)
            }
        }
    }

    suspend internal fun recordLine(count: Int = 1) {
        linesCount += count
        //Logger.d("${context()} record $count, $linesCount")
    }
}
