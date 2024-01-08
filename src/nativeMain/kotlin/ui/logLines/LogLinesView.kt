package ui.logLines

import Config.LogLinesBufferCount
import ServiceLocator
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import ncurses.*
import kotlin.math.min

data class PadPosition(
    val startX: Int,
    val startY: Int,
    val endX: Int,
    val endY: Int,
)

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
class LogLinesView(val position: PadPosition) {

    internal val pad = newpad(LogLinesBufferCount, position.endX)
    private val pageSize = position.endY - position.startY + 1
    private val lastPageSize = pageSize * 2 / 3

    init {
        scrollok(pad, true)
        //keypad(fp, true)
    }

    private var firstVisibleLine = 0

    var snapY = true

    private var linesCount = 0

    fun pageUp() {
        //firstVisibleLine -= pageSize
        //refresh()
        ServiceLocator.appStateFlow.autoscroll(false)

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
        val num = if (ServiceLocator.appStateFlow.state.value.autoscroll) {
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

        ServiceLocator.appStateFlow.autoscroll(false)

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

    fun toLine(line: Int) {
        firstVisibleLine = line
        refresh()
    }

    fun end() {
        firstVisibleLine = linesCount - lastPageSize
        refresh()

        Logger.d("End $firstVisibleLine")
    }

    fun clear() {
        linesCount = 0
        wclear(pad)
        //werase(fp)
       // refresh()
    }

    fun refresh() {
        //Logger.d("FVL $firstVisibleLine")
        prefresh(pad, firstVisibleLine, 0, position.startY, position.startX, position.endY, position.endX)
    }

    fun stop() {
        delwin(pad)
    }

    suspend fun recordLine(count: Int = 1) {
        linesCount += count
        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] record $count, $linesCount")

        //if (snapY) {
        if (ServiceLocator.appStateFlow.state.value.autoscroll) {
            //handle a case when current lines take less than a screen
            end()
        }
    }
}
