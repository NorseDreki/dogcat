package ui.logLines

import Config.LogLinesBufferCount
import ServiceLocator
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*

data class PadPosition(
    val startX: Int,
    val startY: Int,
    val endX: Int,
    val endY: Int,
)

@OptIn(ExperimentalForeignApi::class)
class LogLinesView(val position: PadPosition) {

    val fp = newpad(LogLinesBufferCount, position.endX)
    val pageSize = position.endY - position.startY + 1

    init {
        scrollok(fp, true)
        //keypad(fp, true)
    }

    private var firstVisibleLine = 0

    var snapY = true

    private var linesCount = 0

    fun pageUp() {
        //firstVisibleLine -= pageSize
        //refresh()

        repeat(pageSize) {
            firstVisibleLine--
            refresh()
        }

        Logger.d("Page Up by $pageSize, $firstVisibleLine")
    }

    fun pageDown() {
        //firstVisibleLine += pageSize
        //refresh()

        repeat(pageSize) {
            firstVisibleLine++
            refresh()
        }

        Logger.d("Page Down by $pageSize, $firstVisibleLine")
    }

    fun lineUp() {
        firstVisibleLine--
        refresh()
        Logger.d("Up, $firstVisibleLine")
    }

    fun lineDown() {
        firstVisibleLine++
        refresh()
        Logger.d("Down, $firstVisibleLine")
    }

    fun home() {
        firstVisibleLine = 0
        refresh()
    }

    fun toLine(line: Int) {
        firstVisibleLine = line
        refresh()
    }

    fun end() {
        firstVisibleLine = linesCount// - (position.endY - 1 - position.startY)
        refresh()

        Logger.d("End $firstVisibleLine")
    }

    fun clear() {
        linesCount = 0
        wclear(fp)
        //werase(fp)
       // refresh()
    }

    fun refresh() {
        //Logger.d("FVL $firstVisibleLine")
        //int prefresh(WINDOW *pad, int pminrow, int pmincol,
        //int sminrow, int smincol, int smaxrow, int smaxcol);

        prefresh(fp, firstVisibleLine, 0, position.startY, position.startX, position.endY, position.endX)
        //pnoutrefresh(fp, firstVisibleLine, 0, position.startY, position.startX, position.endY - 1, position.endX)
        //doupdate()
    }

    fun terminate() {
        delwin(fp)
    }

    fun recordLine(count: Int = 1) {
        linesCount += count
        Logger.d("record $count, $linesCount")


        //if (snapY) {
        if (ServiceLocator.appStateFlow.state.value.autoscroll) {
            end()
            //pageUp()
            //pageDown()
        }
    }
}
