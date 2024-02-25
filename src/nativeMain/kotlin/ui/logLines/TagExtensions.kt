package ui.logLines

import AppConfig.DEFAULT_TAG_WIDTH
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.COLOR_PAIR
import ncurses.waddstr
import ncurses.wattroff
import ncurses.wattron
import ui.ColorMap.allocateColor
import ui.ColorMap.COLOR_MAP

@OptIn(ExperimentalForeignApi::class)
internal fun LogLinesView.printTag(tag: String) {
    val color = allocateColor(tag)

    wattron(pad, COLOR_PAIR(COLOR_MAP[color]!!))
    waddstr(pad, tag.procrustes())
    wattroff(pad, COLOR_PAIR(COLOR_MAP[color]!!))
}

private fun String.procrustes() =
    if (length > DEFAULT_TAG_WIDTH) {
        val excess = 1 - DEFAULT_TAG_WIDTH % 2

        take(DEFAULT_TAG_WIDTH / 2 - excess) +
                Typography.ellipsis +
                takeLast(DEFAULT_TAG_WIDTH / 2)
    } else {
        trim().padStart(DEFAULT_TAG_WIDTH)
    }
