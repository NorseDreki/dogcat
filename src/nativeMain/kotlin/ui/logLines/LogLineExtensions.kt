@file:OptIn(ExperimentalForeignApi::class)

package ui.logLines

import dogcat.Brief
import dogcat.LogLevel.*
import dogcat.LogLine
import dogcat.Unparseable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.yield
import logger.Logger
import ncurses.*
import ui.CommonColors
import ui.CommonColors.*
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
suspend fun LogLinesView.processLogLine(
    it: IndexedValue<LogLine>,
) {
    if (it.value is Unparseable) {
        printTag("")
        waddstr(pad, " ".repeat(1 + 3 + 1))
        //account for end of line in the same way as in wrapline
        waddstr(pad, (it.value as Unparseable).line + "\n")
        recordLine(1)

        return
    }

    val logLine = it.value as Brief //do not cast
    printTag(logLine.tag)

    val wrappedLine = wrapLine("${it.index} ${logLine.message}")
    val wrapped = wrappedLine.first
    recordLine(wrappedLine.second)

    when (val level = logLine.level) {
        W -> {
            printLevelAndMessage(level.name, BLACK_ON_YELLOW.colorPairCode, wrapped, COLOR_PAIR(YELLOW_ON_BG.colorPairCode))
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

private fun LogLinesView.printLevelAndMessage(
    level: String,
    levelColorPair: Int,
    message: String,
    messageColorPair: Int,
) {
    waddstr(pad, " ")

    wattron(pad, COLOR_PAIR(levelColorPair));
    waddstr(pad, " $level ")
    wattroff(pad, COLOR_PAIR(levelColorPair));

    waddstr(pad, " ")

    wattron(pad, messageColorPair)
    waddstr(pad, message)
    wattroff(pad, messageColorPair)
}

private fun LogLinesView.wrapLine(
    message: String
): Pair<String, Int> {

    val r = """[\t\n\r\\b\f\v\a\e]""".toRegex()

    val width = position.endX
    val header = AppConfig.DEFAULT_TAG_WIDTH + 1 + 3 + 1// space, level, space
    val line = message.replace(r, " ")
    val wrapArea = width - header
    var buf = ""
    var current = 0

    var count = 1

    while (current < line.length) {
        val next = min(current + wrapArea, line.length)
        buf += line.substring(current, next)
        if (next < line.length) {
            count += 1
            buf += " ".repeat(header) //
        }
        current = next
    }

    val sx = getmaxx(pad)

    val s = if ((buf.length + header) % sx == 0) {
        buf
    } else {
        buf + "\n"
    }

    return s to count
}
