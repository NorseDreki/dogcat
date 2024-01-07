@file:OptIn(
    ExperimentalForeignApi::class,
    ExperimentalForeignApi::class, ExperimentalForeignApi::class
)

package ui.logLines

import Config.tagWidth
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*

internal fun LogLinesView.printTag(tag: String) {
    val c = allocateColor(tag)

    wattron(pad, COLOR_PAIR(ccpp[c]!!))
    waddstr(pad, tag.massage())
    wattroff(pad, COLOR_PAIR(ccpp[c]!!))
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

private val LAST_USED = mutableListOf(
    /*COLOR_RED,*/
    COLOR_GREEN,
    /*COLOR_YELLOW,*/
    COLOR_BLUE,
    COLOR_MAGENTA,
    COLOR_CYAN
)

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

private val ccpp = (LAST_USED + KNOWN_TAGS.values)
    .associateWith {
        init_pair((100 + it).toShort(), it.toShort(), -1)
        (100 + it)
    }

private fun String.massage() = //prokrust
    if (length > tagWidth) {
        val excess = 1 - tagWidth % 2
        // performance impact?
        take(tagWidth / 2 - excess) +
                Typography.ellipsis +
                takeLast(tagWidth / 2)
    } else {
        trim().padStart(tagWidth)
    }
