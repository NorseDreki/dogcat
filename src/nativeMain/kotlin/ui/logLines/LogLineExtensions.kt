package ui.logLines

import AppConfig.LOG_LEVEL_WIDTH
import AppConfig.LOG_LINE_ESCAPE_REGEX_STRING
import dogcat.Brief
import dogcat.LogLevel.*
import dogcat.LogLine
import dogcat.Unparseable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.yield
import ncurses.*
import ui.CommonColors.*
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
suspend fun LogLinesView.processLogLine(
    it: IndexedValue<LogLine>,
) {
    if (it.value is Unparseable) {
        printTag("")
        waddstr(pad, " ".repeat(LOG_LEVEL_WIDTH))

        //account for end of line in the same way as in wrapline
        waddstr(pad, (it.value as Unparseable).line + "\n")
        recordLine(1)

        return
    }

    val logLine = it.value as Brief //do not cast
    printTag(logLine.tag)

    val line =
        if (state.showLineNumbers) "-${it.index}- ${logLine.message}"
        else logLine.message

    val wrappedLine = wrapLine(line)
    val wrapped = wrappedLine.first
    recordLine(wrappedLine.second)

    when (val level = logLine.level) {
        W -> {
            printLevelAndMessage(
                level.name,
                BLACK_ON_YELLOW.colorPairCode,
                wrapped,
                COLOR_PAIR(YELLOW_ON_BG.colorPairCode)
            )
        }

        E, F -> {
            printLevelAndMessage(level.name, BLACK_ON_RED.colorPairCode, wrapped, COLOR_PAIR(RED_ON_BG.colorPairCode))
        }

        I -> {
            printLevelAndMessage(level.name, BLACK_ON_WHITE.colorPairCode, wrapped, A_BOLD.toInt())
        }

        else -> {
            printLevelAndMessage(level.name, BLACK_ON_WHITE.colorPairCode, wrapped, 0)
        }
    }

    if (state.autoscroll) {
        //handle a case when current lines take less than a screen
        //end()
        if (linesCount < pageSize) {
            refresh()
        } else {
            lineDown(wrappedLine.second) //batch calls in order not to draw each line
        }
    } else {
        refresh()
    }

    yield()
}

@OptIn(ExperimentalForeignApi::class)
private fun LogLinesView.printLevelAndMessage(
    level: String,
    levelColorPair: Int,
    message: String,
    messageColorPair: Int,
) {
    waddstr(pad, " ")

    wattron(pad, COLOR_PAIR(levelColorPair))
    waddstr(pad, " $level ")
    wattroff(pad, COLOR_PAIR(levelColorPair))

    waddstr(pad, " ")

    wattron(pad, messageColorPair)
    waddstr(pad, message)
    wattroff(pad, messageColorPair)
}

/**
 * @return A pair of wrapped line (with correctly placed EOL) and number of screen lines it takes.
 */
private fun LogLinesView.wrapLine(
    message: String
): Pair<String, Int> {

    val escapeRegex = LOG_LINE_ESCAPE_REGEX_STRING.toRegex()
    val line = message.replace(escapeRegex, " ")

    val width = position.endX
    val header = state.tagWidth + LOG_LEVEL_WIDTH
    val wrapArea = width - header

    var lineBuffer = ""
    var current = 0
    var linesCount = 1

    while (current < line.length) {
        val next = min(current + wrapArea, line.length)

        // TOP LEVEL CATCH! kotlin.ArrayIndexOutOfBoundsException null
        lineBuffer += line.substring(current, next)

        if (next < line.length) {
            linesCount += 1
            lineBuffer += " ".repeat(header)
        }

        current = next
    }

    val fitsWidthPrecisely = (lineBuffer.length + header) % sx == 0

    val lineBufferPlus =
        if (fitsWidthPrecisely) {
            lineBuffer
        } else {
            lineBuffer + "\n"
        }

    return lineBufferPlus to linesCount
}
