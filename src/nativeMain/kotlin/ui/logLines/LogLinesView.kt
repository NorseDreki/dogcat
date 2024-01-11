package ui.logLines

import dogcat.DogcatConfig.MAX_LOG_LINES
import DogcatModule
import kotlinx.cinterop.ExperimentalForeignApi
import logger.Logger
import logger.context
import ncurses.*
import ui.ViewPosition
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
class LogLinesView {
    private val sx = getmaxx(stdscr)
    private val sy = getmaxy(stdscr)

    internal val position = ViewPosition(0, 0, sx, sy - 4) //- 5)


    internal val pad = newpad(MAX_LOG_LINES, position.endX)
    private val pageSize = position.endY - position.startY + 1
    private val lastPageSize = pageSize - 1 // * 9 / 10

    init {
        scrollok(pad, true)
        //keypad(fp, true)
    }

    private var firstVisibleLine = 0
    private var linesCount = 0

    fun pageUp() {
        //firstVisibleLine -= pageSize
        //refresh()
        DogcatModule.appStateFlow.autoscroll(false)

        val num = min(pageSize, firstVisibleLine)

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

        val num = if (DogcatModule.appStateFlow.state.value.autoscroll) {
            min(pageSize, linesCount - lastPageSize - firstVisibleLine)
        } else {
            min(pageSize, linesCount - firstVisibleLine)
        }

        //val num = min(pageSize, linesCount - firstVisibleLine)

        repeat(num) {
            firstVisibleLine++
            refresh()
        }

        Logger.d("Page Down by $pageSize, $firstVisibleLine")
    }

    fun lineUp() {
        if (firstVisibleLine == 0) return

        DogcatModule.appStateFlow.autoscroll(false)

        firstVisibleLine--
        refresh()
        Logger.d("Up, $firstVisibleLine")
    }

    fun lineDown() {
        if (firstVisibleLine == linesCount) return

        firstVisibleLine++
        refresh()
        Logger.d("Down, $firstVisibleLine")
    }

    fun home() {
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
        //to few lines for page down
        if (linesCount <= pageSize) {
            return
        }

        prefresh(pad, linesCount, 0, position.startY, position.startX, position.endY, position.endX)

        firstVisibleLine = linesCount - lastPageSize
        //refresh()

        Logger.d("End $firstVisibleLine")
    }

    suspend fun clear() {
        prefresh(pad, linesCount, 0, position.startY, position.startX, position.endY, position.endX)

        wclear(pad)
        linesCount = 0
        firstVisibleLine = 0

        Logger.d("${context()} Cleared pad")
        //werase(fp)
       // refresh()

        curs_set(1)
    }

    fun refresh() {
        //logger.Logger.d("FVL $firstVisibleLine")

        if (firstVisibleLine <= linesCount - pageSize) {
            curs_set(0)
        } else {
            curs_set(1)
        }

        prefresh(pad, firstVisibleLine, 0, position.startY, position.startX, position.endY, position.endX)
    }

    fun stop() {
        delwin(pad)
    }

    suspend fun recordLine(count: Int = 1) {
        linesCount += count
        Logger.d("${context()} record $count, $linesCount")

        //if (snapY) {
        if (DogcatModule.appStateFlow.state.value.autoscroll) {
            //handle a case when current lines take less than a screen
            end()
        }
    }
}
