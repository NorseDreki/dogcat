/*
 * SPDX-FileCopyrightText: Copyright 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.app

import com.norsedreki.dogcat.DogcatException
import com.norsedreki.dogcat.app.AppConfig.DEFAULT_LOCALE
import com.norsedreki.dogcat.app.ui.CommonColors
import com.norsedreki.dogcat.app.ui.HasLifecycle
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.endwin
import ncurses.has_colors
import ncurses.init_pair
import ncurses.initscr
import ncurses.keypad
import ncurses.nodelay
import ncurses.noecho
import ncurses.start_color
import ncurses.stdscr
import ncurses.use_default_colors
import platform.posix.LC_ALL
import platform.posix.setlocale

@OptIn(ExperimentalForeignApi::class)
class AppView : HasLifecycle {

    override suspend fun start() {
        setlocale(LC_ALL, DEFAULT_LOCALE)
        initscr()

        keypad(stdscr, true)
        noecho()

        // The nodelay option causes getch to be a non-blocking call. If no input is ready, getch returns ERR.
        // If disabled (bf is FALSE), getch waits until a key is pressed
        nodelay(stdscr, true)
        // cbreak or raw, to make wgetch read unbuffered data, i.e., not waiting for '\n'.

        if (!has_colors()) {
            throw DogcatException(
                "As of now, the app can only run on a terminal which supports colors. " +
                    "It seems this terminal doesn't.",
            )
        }

        use_default_colors()
        start_color()

        CommonColors.entries.forEach {
            init_pair(it.colorPairCode.toShort(), it.foregroundColor, it.backgroundColor)
        }
    }

    override suspend fun stop() {
        endwin()
    }
}
