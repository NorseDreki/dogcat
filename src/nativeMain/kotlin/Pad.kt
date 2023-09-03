import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*

@OptIn(ExperimentalForeignApi::class)
class Pad(val position: PadPosition) {

    val fp = newpad(Config.LogLinesBufferCount, position.endX)

    init {
        scrollok(fp, true)
        //keypad(fp, true)
    }

    private var firstVisibleLine = 0

    val snapY = false

    private var linesCount = 0

    fun pageUp() {
        firstVisibleLine -= position.endY - 1 - position.startY
        refresh()
    }

    fun pageDown() {
        firstVisibleLine += position.endY - 1 - position.startY
        refresh()
    }

    fun lineUp() {
        firstVisibleLine--
        refresh()
    }

    fun lineDown() {
        firstVisibleLine++
        refresh()
    }

    fun home() {
        firstVisibleLine = 0
        refresh()
    }

    fun end() {
        firstVisibleLine = linesCount - (position.endY - 1 - position.startY)
        refresh()
    }

    fun clear() {
        linesCount = 0
        wclear(fp)
    }

    fun refresh() {
        prefresh(fp, firstVisibleLine, 0, position.startY, position.startX, position.endY - 1, position.endX)
    }

    fun terminate() {
        delwin(fp)
    }

    fun recordLine() {
        linesCount++
    }
}