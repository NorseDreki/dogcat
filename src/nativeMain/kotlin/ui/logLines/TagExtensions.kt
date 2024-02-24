@file:OptIn(
    ExperimentalForeignApi::class,
)

package ui.logLines

import AppConfig.DEFAULT_TAG_WIDTH
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*
import ui.ColorMap.allocateColor
import ui.ColorMap.ccpp

internal fun LogLinesView.printTag(tag: String) {
    val c = allocateColor(tag)

    wattron(pad, COLOR_PAIR(ccpp[c]!!))
    waddstr(pad, tag.massage())
    wattroff(pad, COLOR_PAIR(ccpp[c]!!))
}

private fun String.massage() = //prokrust
    if (length > DEFAULT_TAG_WIDTH) {
        val excess = 1 - DEFAULT_TAG_WIDTH % 2
        // performance impact?
        take(DEFAULT_TAG_WIDTH / 2 - excess) +
                Typography.ellipsis +
                takeLast(DEFAULT_TAG_WIDTH / 2)
    } else {
        trim().padStart(DEFAULT_TAG_WIDTH)
    }
