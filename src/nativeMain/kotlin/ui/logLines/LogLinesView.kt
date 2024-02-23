package ui.logLines

import dogcat.DogcatConfig.MAX_LOG_LINES
import kotlinx.cinterop.ExperimentalForeignApi
import logger.Logger
import logger.context
import ncurses.*
import ui.ViewPosition
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
class LogLinesView {
    var isCursorHeld: Boolean = false

    private val sx = getmaxx(stdscr)
    private val sy = getmaxy(stdscr)

    internal val position = ViewPosition(0, 0, sx, sy - 4)

    internal val pad = newpad((MAX_LOG_LINES * 1.3).toInt(), position.endX) //fix guesstimation
    internal val pageSize = position.endY - position.startY + 1
    private val lastPageSize = pageSize - 1

    internal var autoscroll = false

    init {
        scrollok(pad, true)

        //does it even work?
//        leaveok(pad, true);


        //WINDOW * win1 = newwin(10, 40, 0, 0)
        //WINDOW * win2 = newwin(10, 40, 0, 40)


// Get the current position of the cursor in win1
        //var y: Int
        //var x: Int
        //getyx(win1, y, x)


// Write a string to win2
        //waddstr(win2, "Hello, world!")


// Move the cursor back to its previous position in win1
        //wmove(win1, y, x)

        //keypad(fp, true)
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

    // useful for bookmarking then quickly moving between marked lines
    fun toLine(line: Int) {
        firstVisibleLine = line
        refresh()
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

    fun refresh() {
        //logger.Logger.d("FVL $firstVisibleLine")
        val notSeeingLastLine = firstVisibleLine <= linesCount - pageSize

        /**/

/*        when {
            isCursorHeld -> {
                Logger.d("cursor is held")
                //curs_set(0)
            }
            notSeeingLastLine -> {
                curs_set(0)
            }
            else -> {
                curs_set(1)
            }
        }*/

        prefresh(pad, firstVisibleLine, 0, position.startY, position.startX, position.endY, position.endX)
        //call doupdate with pnoutrefresh

        when {
            isCursorHeld -> {
                wmove(stdscr, 49, "Filter: ".length)
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

        /*if (!isCursorHeld && !notSeeingLastLine) {
            curs_set(1)
        } else {
            curs_set(0)
        }

        if (isCursorHeld) {
            wmove(stdscr, 49, "Filter: ".length)
            curs_set(1)
            wrefresh(stdscr)
        }*/
    }

    suspend fun recordLine(count: Int = 1) {
        linesCount += count
        //Logger.d("${context()} record $count, $linesCount")
    }
}
