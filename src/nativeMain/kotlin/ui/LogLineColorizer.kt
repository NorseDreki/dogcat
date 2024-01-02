@file:OptIn(ExperimentalForeignApi::class)

package ui

import Config
import Config.tagWidth
import dogcat.LogLine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.yield
import ncurses.*
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
private fun printTag(pad: Pad, tag: String) {
    val c = allocateColor(tag)

    wattron(pad.fp, COLOR_PAIR(ccpp[c]!!))
    waddstr(pad.fp, tag.massage())
    wattroff(pad.fp, COLOR_PAIR(ccpp[c]!!))
}

private fun allocateColor(tag: String): Int {
    if (!KNOWN_TAGS.containsKey(tag)) {
        KNOWN_TAGS[tag] = LAST_USED[0]
    }
    val color = KNOWN_TAGS[tag]!!

    if (LAST_USED.contains(color)) {
        LAST_USED.remove(color)
        LAST_USED.add(color)
    }

    return color
}

private val LAST_USED = mutableListOf( /*COLOR_RED,*/ COLOR_GREEN, /*COLOR_YELLOW,*/ COLOR_BLUE, COLOR_MAGENTA, COLOR_CYAN)

private val KNOWN_TAGS = mutableMapOf(
    "dalvikvm" to COLOR_WHITE,
    "Process" to COLOR_WHITE,
    "ActivityManager" to COLOR_WHITE,
    "ActivityThread" to COLOR_WHITE,
    "AndroidRuntime" to COLOR_CYAN,
    "jdwp" to COLOR_WHITE,
    "StrictMode" to COLOR_WHITE,
    "DEBUG" to COLOR_YELLOW,
)

private val ccpp = (LAST_USED + KNOWN_TAGS.values).map {
    init_pair((100 + it).toShort(), it.toShort(), -1)
    it to (100 + it)
}.toMap()

private fun String.massage(): String {
    return if (length > tagWidth) {
        val excess = 1 - tagWidth % 2
        // performance impact?
        take(tagWidth / 2 - excess) + Typography.ellipsis + takeLast(tagWidth / 2)
    } else {
        trim().padStart(tagWidth)
    }
}

@OptIn(ExperimentalForeignApi::class)
suspend fun Pad.processLogLine(
    it: IndexedValue<LogLine>,
) {
    //waddstr(fp, "${it.index} ")
    val logLine = it.value

    printTag(this, logLine.tag)

    waddstr(fp, " ")

    val wrappedLine  = wrapLine(this, "${it.index} ${logLine.message}")
    val wrapped = wrappedLine.first

    /*wrapped.lines().withIndex().forEach {
        Logger.d("${it.index} ${it.value}")
    }*/

    recordLine(wrappedLine.second)

    when (logLine.level) {
        "W" -> {
            wattron(fp, COLOR_PAIR(6));
            waddstr(fp, " ${logLine.level} ")
            wattroff(fp, COLOR_PAIR(6));

            waddstr(fp, " ")

            wattron(fp, COLOR_PAIR(3))
            waddstr(fp, wrapped)
            wattroff(fp, COLOR_PAIR(3))
        }

        "E", "F" -> {
            wattron(fp, COLOR_PAIR(11))
            waddstr(fp, " ${logLine.level} ")
            wattroff(fp, COLOR_PAIR(11))

            waddstr(fp, " ")

            wattron(fp, COLOR_PAIR(1))
            waddstr(fp, wrapped)
            wattroff(fp, COLOR_PAIR(1));
        }

        "I" -> {
            wattron(fp, COLOR_PAIR(12))
            waddstr(fp, " ${logLine.level} ")
            wattroff(fp, COLOR_PAIR(12))

            waddstr(fp, " ")

            wattron(fp, A_BOLD.toInt())
            waddstr(fp, wrapped)
            wattroff(fp, A_BOLD.toInt())
        }

        else -> {
            wattron(fp, COLOR_PAIR(12))
            waddstr(fp, " ${logLine.level} ")
            wattroff(fp, COLOR_PAIR(12))

            waddstr(fp, " ")

            //wattron(fp, A_DIM.toInt())
            waddstr(fp, wrapped)
            //wattroff(fp, A_DIM.toInt())
        }
        //waddstr(fp, "${it.value.message} \n")
    }
     yield()

    //pad.recordLine()
    //pad.refresh()
}

private fun wrapLine(
    pad: Pad,
    message: String
): Pair<String, Int> {
    val width = pad.position.endX
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
