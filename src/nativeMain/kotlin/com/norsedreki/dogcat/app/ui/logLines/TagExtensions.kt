/*
 * SPDX-FileCopyrightText: Copyright 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.logLines

import com.norsedreki.dogcat.app.AppConfig.TAG_COLOR_PAIR_OFFSET
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.COLOR_BLUE
import ncurses.COLOR_CYAN
import ncurses.COLOR_GREEN
import ncurses.COLOR_MAGENTA
import ncurses.COLOR_PAIR
import ncurses.COLOR_WHITE
import ncurses.COLOR_YELLOW
import ncurses.init_pair
import ncurses.waddstr
import ncurses.wattroff
import ncurses.wattron

@OptIn(ExperimentalForeignApi::class)
internal fun LogLinesView.printTag(tag: String) {
    val color = allocateColor(tag)

    wattron(pad, COLOR_PAIR(TAG_COLOR_MAP[color]!!))
    waddstr(pad, procrustes(tag))
    wattroff(pad, COLOR_PAIR(TAG_COLOR_MAP[color]!!))
}

private fun LogLinesView.procrustes(tag: String) =
    if (tag.length > state.tagWidth) {
        val excess = 1 - state.tagWidth % 2

        tag.take(state.tagWidth / 2 - excess) +
            Typography.ellipsis +
            tag.takeLast(state.tagWidth / 2)
    } else {
        tag.trim().padStart(state.tagWidth)
    }

private fun allocateColor(tag: String): Int {
    if (!KNOWN_TAG_COLORS.containsKey(tag)) {
        KNOWN_TAG_COLORS[tag] = LAST_USED_TAG_COLORS[0]
    }
    val color = KNOWN_TAG_COLORS[tag]!!

    if (LAST_USED_TAG_COLORS.contains(color)) {
        LAST_USED_TAG_COLORS.remove(color)
        LAST_USED_TAG_COLORS.add(color)
    }

    return color
}

@OptIn(ExperimentalForeignApi::class)
private val LAST_USED_TAG_COLORS = mutableListOf(
    COLOR_GREEN,
    COLOR_BLUE,
    COLOR_MAGENTA,
    COLOR_CYAN,
)

@OptIn(ExperimentalForeignApi::class)
private val KNOWN_TAG_COLORS = mutableMapOf(
    "EGL_emulation" to COLOR_WHITE,
    "OpenGLRenderer" to COLOR_WHITE,
    "Looper" to COLOR_WHITE,
    "EventThread" to COLOR_WHITE,
    "Finsky" to COLOR_WHITE,
    "Choreographer" to COLOR_WHITE,
    // Later, add more handling to these special cases to stand them out
    "StrictMode" to COLOR_WHITE,
    "DEBUG" to COLOR_YELLOW,
)

@OptIn(ExperimentalForeignApi::class)
private val TAG_COLOR_MAP by lazy {
    (LAST_USED_TAG_COLORS + KNOWN_TAG_COLORS.values)
        .associateWith {
            val nextColorPair = TAG_COLOR_PAIR_OFFSET + it
            init_pair(nextColorPair.toShort(), it.toShort(), -1)

            nextColorPair
        }
}
