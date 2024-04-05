/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui

import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.COLOR_BLACK
import ncurses.COLOR_GREEN
import ncurses.COLOR_RED
import ncurses.COLOR_WHITE
import ncurses.COLOR_YELLOW

@ExperimentalForeignApi
enum class CommonColors(
    val colorPairCode: Int,
    val foregroundColor: Short,
    val backgroundColor: Short
) {
    RED_ON_BG(1, COLOR_RED.toShort(), -1),
    GREEN_ON_BG(2, COLOR_GREEN.toShort(), -1),
    YELLOW_ON_BG(3, COLOR_YELLOW.toShort(), -1),
    RED_ON_WHITE(4, COLOR_RED.toShort(), COLOR_WHITE.toShort()),
    GREEN_ON_WHITE(5, COLOR_GREEN.toShort(), COLOR_WHITE.toShort()),
    BLACK_ON_WHITE(6, COLOR_BLACK.toShort(), COLOR_WHITE.toShort()),
    BLACK_ON_RED(7, COLOR_BLACK.toShort(), COLOR_RED.toShort()),
    BLACK_ON_YELLOW(8, COLOR_BLACK.toShort(), COLOR_YELLOW.toShort())
}
