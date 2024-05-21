/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.logLines

import com.norsedreki.dogcat.Brief
import com.norsedreki.dogcat.LogLevel.E
import com.norsedreki.dogcat.LogLevel.F
import com.norsedreki.dogcat.LogLevel.I
import com.norsedreki.dogcat.LogLevel.W
import com.norsedreki.dogcat.LogLine
import com.norsedreki.dogcat.Unparseable
import com.norsedreki.dogcat.app.AppConfig.LOG_LEVEL_WIDTH
import com.norsedreki.dogcat.app.ui.CommonColors.BLACK_ON_RED
import com.norsedreki.dogcat.app.ui.CommonColors.BLACK_ON_WHITE
import com.norsedreki.dogcat.app.ui.CommonColors.BLACK_ON_YELLOW
import com.norsedreki.dogcat.app.ui.CommonColors.RED_ON_BG
import com.norsedreki.dogcat.app.ui.CommonColors.YELLOW_ON_BG
import kotlin.math.min
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.yield
import ncurses.A_BOLD
import ncurses.COLOR_PAIR
import ncurses.waddstr
import ncurses.wattroff
import ncurses.wattron

@OptIn(ExperimentalForeignApi::class)
suspend fun LogLinesView.printLogLine(logLine: IndexedValue<LogLine>) {
    if (logLine.value is Unparseable) {
        // pad tag area 1
        printTag("")

        // pad level area
        waddstr(pad, " ".repeat(LOG_LEVEL_WIDTH))

        // display unparseable log line in place of a message
        val line = (logLine.value as Unparseable).line
        waddstr(pad, line + "\n")

        incrementLinesCount(1)
        return
    }

    val numLinesOnScreen = printBriefLogLine(logLine)

    refreshPrintedLine(numLinesOnScreen)

    yield()
}

private fun LogLinesView.refreshPrintedLine(numLinesOnScreen: Int) {
    if (state.autoscroll) {
        val isOverscrollWithinLastPage =
            state.overscroll && linesCount - firstVisibleLine <= pageSize

        if (linesCount < pageSize || isOverscrollWithinLastPage) {
            refresh()
        } else {
            lineDown(numLinesOnScreen) // batch calls in order not to draw each line
        }
    } else {
        if (state.overscroll && firstVisibleLine >= numLinesOnScreen) {
            firstVisibleLine -= numLinesOnScreen
        }
        refresh()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun LogLinesView.printBriefLogLine(logLine: IndexedValue<LogLine>): Int {
    val briefLogLine = logLine.value as Brief
    printTag(briefLogLine.tag)

    val message =
        if (state.showLineNumbers) {
            "-${logLine.index}- ${briefLogLine.message}"
        } else {
            briefLogLine.message
        }

    val wrappedMessageAndCount = wrapLine(message)
    val wrappedMessage = wrappedMessageAndCount.first
    val linesCount = wrappedMessageAndCount.second

    incrementLinesCount(linesCount)

    when (val level = briefLogLine.level) {
        W -> {
            printLevelAndMessage(
                level.name,
                BLACK_ON_YELLOW.colorPairCode,
                wrappedMessage,
                COLOR_PAIR(YELLOW_ON_BG.colorPairCode),
            )
        }
        E,
        F -> {
            printLevelAndMessage(
                level.name,
                BLACK_ON_RED.colorPairCode,
                wrappedMessage,
                COLOR_PAIR(RED_ON_BG.colorPairCode),
            )
        }
        I -> {
            printLevelAndMessage(
                level.name,
                BLACK_ON_WHITE.colorPairCode,
                wrappedMessage,
                A_BOLD.toInt()
            )
        }
        else -> {
            printLevelAndMessage(level.name, BLACK_ON_WHITE.colorPairCode, wrappedMessage, 0)
        }
    }

    return linesCount
}

@OptIn(ExperimentalForeignApi::class)
private fun LogLinesView.printLevelAndMessage(
    level: String,
    levelColorPair: Int,
    message: String,
    messageColorPair: Int
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
private fun LogLinesView.wrapLine(inputLine: String): Pair<String, Int> {
    val escapeRegex = """[\t\n\r\\b\f\v\a\e]""".toRegex()
    val line = inputLine.replace(escapeRegex, " ")

    val width = endX
    val header = state.tagWidth + LOG_LEVEL_WIDTH
    val wrapArea = width - header

    var lineBuffer = ""
    var current = 0
    var linesCount = 1

    while (current < line.length) {
        val next = min(current + wrapArea, line.length)

        lineBuffer += line.substring(current, next)

        if (next < line.length) {
            linesCount += 1
            lineBuffer += " ".repeat(header)
        }

        current = next
    }

    val fitsWidthPrecisely = (lineBuffer.length + header) % endX == 0

    val lineBufferResult =
        if (fitsWidthPrecisely) {
            lineBuffer
        } else {
            lineBuffer + "\n"
        }

    return lineBufferResult to linesCount
}
