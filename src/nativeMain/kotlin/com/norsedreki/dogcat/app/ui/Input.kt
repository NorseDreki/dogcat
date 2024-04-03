/*
 * SPDX-FileCopyrightText: Copyright (C) 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui

import com.norsedreki.dogcat.app.AppConfig.INPUT_KEY_DELAY_MILLIS
import com.norsedreki.dogcat.app.AppState
import com.norsedreki.dogcat.app.Keymap
import com.norsedreki.dogcat.app.Keymap.Actions.INPUT_FILTER_BY_SUBSTRING
import com.norsedreki.dogcat.app.ui.Strings.INPUT_FILTER_PREFIX
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlin.coroutines.coroutineContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ncurses.ERR
import ncurses.KEY_BACKSPACE
import ncurses.KEY_LEFT
import ncurses.KEY_RIGHT
import ncurses.curs_set
import ncurses.getmaxy
import ncurses.mvaddch
import ncurses.mvdelch
import ncurses.mvwprintw
import ncurses.stdscr
import ncurses.wclrtoeol
import ncurses.wgetch
import ncurses.wmove
import ncurses.wrefresh

interface Input : HasLifecycle {

    val keypresses: Flow<Int>

    val strings: Flow<String>
}

class DefaultInput(private val appState: AppState) : Input {

    private val keypressesSubject = MutableSharedFlow<Int>()
    override val keypresses = keypressesSubject

    private val stringsSubject = MutableSharedFlow<String>()
    override val strings = stringsSubject

    private var inputMode = false
    private var inputBuffer = StringBuilder()

    private val inputX = INPUT_FILTER_PREFIX.length

    @OptIn(ExperimentalForeignApi::class) private val inputY by lazy { getmaxy(stdscr) - 1 }

    private var cursorPosition = inputX

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun start() {
        CoroutineScope(coroutineContext).launch {
            appState.setUserInputLocation(inputX, inputY)
            mvwprintw(stdscr, inputY, 0, INPUT_FILTER_PREFIX)

            while (isActive) {
                val key = wgetch(stdscr)

                if (key == ERR) {
                    delay(INPUT_KEY_DELAY_MILLIS)
                    continue
                }

                if (Keymap.bindings[key] == INPUT_FILTER_BY_SUBSTRING && !inputMode) {
                    enterInputMode()
                    continue
                }

                if (inputMode) {
                    processKeyInInputMode(key)
                } else {
                    Logger.d("${context()} Process key $key")
                    keypressesSubject.emit(key)
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun enterInputMode() {
        appState.holdCursor(true)
        inputMode = true

        wmove(stdscr, inputY, inputX)
        wclrtoeol(stdscr)

        wmove(stdscr, inputY, cursorPosition)
        curs_set(1)
        wrefresh(stdscr)
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun processKeyInInputMode(key: Int) {
        when (key) {
            KEY_LEFT -> {
                if (cursorPosition - inputX > 0) cursorPosition--
            }
            KEY_RIGHT -> {
                if (cursorPosition - inputX < inputBuffer.length) cursorPosition++
            }
            KEY_BACKSPACE,
            127 -> {
                if (cursorPosition - inputX > 0) {
                    inputBuffer.deleteAt(cursorPosition - inputX - 1)
                    cursorPosition--

                    mvdelch(inputY, cursorPosition)
                }
            }
            '\n'.code -> {
                val input = inputBuffer.toString()
                inputBuffer.clear()
                cursorPosition = inputX
                curs_set(0)

                inputMode = false
                appState.holdCursor(false)

                stringsSubject.emit(input)
            }
            27 -> { // ESCAPE
                mvwprintw(stdscr, inputY, inputX, " ".repeat(inputBuffer.length))

                inputBuffer.clear()
                cursorPosition = inputX
            }
            else -> {
                val char = key.toChar()

                if (char in ' '..'~') {
                    inputBuffer.insert(cursorPosition - inputX, char)

                    mvaddch(inputY, cursorPosition, key.toUInt())

                    cursorPosition++
                }
            }
        }

        wmove(stdscr, inputY, cursorPosition)
        appState.setUserInputLocation(cursorPosition, inputY)
    }

    override suspend fun stop() {
        // will be stopped by CancellationException
    }
}
