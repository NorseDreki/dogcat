import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import ncurses.*
import platform.Logger

data class PadPosition(
    val startX: Int,
    val startY: Int,
    val endX: Int,
    val endY: Int,
)

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
        /*firstVisibleLine -= position.endY - 1 - position.startY
        refresh()*/

        (1..53).forEach {
            firstVisibleLine--
            refresh()
        }
    }

    suspend fun pageDown() {
        /*firstVisibleLine += position.endY - 1 - position.startY
        //delay(100)
        refresh()
*/
        (1..53).forEach {
            firstVisibleLine++
            refresh()
        }
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

    fun toLine(line: Int) {
        firstVisibleLine = line
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
        Logger.d("FVL $firstVisibleLine")

        prefresh(fp, firstVisibleLine, 0, position.startY, position.startX, position.endY - 1, position.endX)
        //pnoutrefresh(fp, firstVisibleLine, 0, position.startY, position.startX, position.endY - 1, position.endX)
        //doupdate()
    }

    fun terminate() {
        delwin(fp)
    }

    fun recordLine() {
        linesCount++
    }
}
