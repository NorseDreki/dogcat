@file:OptIn(ExperimentalForeignApi::class)

package ui.logLines

import Config
import dogcat.BriefLogLine
import dogcat.LogLine
import dogcat.UnparseableLogLine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.yield
import ncurses.*
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
suspend fun LogLinesView.processLogLine(
    it: IndexedValue<LogLine>,
) {
    if (it.value is UnparseableLogLine) {
        printTag("")
        //use System.line ending
        waddstr(pad, " ".repeat(1 + 3 + 1))
        waddstr(pad, (it.value as UnparseableLogLine).line + "\n")
        return
    }

    val logLine = it.value as BriefLogLine //do not cast
    printTag(logLine.tag)

    val wrappedLine = wrapLine("${it.index} ${logLine.message}")
    val wrapped = wrappedLine.first
    recordLine(wrappedLine.second)

    when (logLine.level) {
        "W" -> {
            printLevelAndMessage(logLine.level, 6, wrapped, COLOR_PAIR(3))
        }
        "E", "F" -> {
            printLevelAndMessage(logLine.level, 11, wrapped, COLOR_PAIR(1))
        }
        "I" -> {
            printLevelAndMessage(logLine.level, 12, wrapped, A_BOLD.toInt())
        }
        else -> {
            printLevelAndMessage(logLine.level, 12, wrapped, 0)
        }
    }

    refresh()
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
    val width = position.endX
    val header = Config.tagWidth + 1 + 3 + 1// space, level, space
    val line = message.replace("\t", "    ") //prevent escape characters leaking
    val wrapArea = width - header
    var buf = ""
    var current = 0

    var count = 1

    while (current < line.length) {
        val next = min(current + wrapArea, line.length)
        buf += line.substring(current, next)
        if (next < line.length) {
            //buf += "\n"
            count += 1
            buf += " ".repeat(header) //
        }
        current = next
    }
    return buf + "\n" to count //+ "\n\r"
}
