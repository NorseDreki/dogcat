package userInput

import AppConfig.INPUT_KEY_DELAY_MILLIS
import AppState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import logger.Logger
import logger.context
import ncurses.*
import ui.HasLifecycle
import kotlin.coroutines.coroutineContext

interface Input : HasLifecycle {
    val keypresses: Flow<Int>

    val strings: Flow<String>
}

class DefaultInput(
    private val appState: AppState
) : Input {

    private val keypressesSubject = MutableSharedFlow<Int>()
    override val keypresses = keypressesSubject

    private val stringsSubject = MutableSharedFlow<String>()
    override val strings = stringsSubject

    private var inputMode = false
    private var inputBuffer = StringBuilder()

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun start() {
        val x = appState.state.value.inputFilterLocation.first
        val y = appState.state.value.inputFilterLocation.second

        CoroutineScope(coroutineContext)
            .launch {
                var cursorPosition = x

                while (isActive) {
                    val key = wgetch(stdscr)

                    if (key == ERR) {
                        /*if (inputMode) {
                            wmove(stdscr, y , cursorPosition)
                            curs_set(1)

                            wrefresh(stdscr)
                        }*/

                        delay(INPUT_KEY_DELAY_MILLIS)

                        continue
                    }

                    if (Keymap.bindings[key] == Keymap.Actions.InputFilterBySubstring && !inputMode) {
                        appState.holdCursor(true)
                        inputMode = true

                        mvwaddstr(stdscr, y, 0, "Filter: ")
                        wclrtoeol(stdscr)

                        //move not needed?
                        wmove(stdscr, y , cursorPosition)
                        curs_set(1)

                        wrefresh(stdscr)

                        continue
                    }


                    // limit max input
                    if (inputMode) {
                        when (key) {
                            KEY_LEFT -> if (cursorPosition - x > 0) cursorPosition--

                            KEY_RIGHT -> if (cursorPosition - x < inputBuffer.length) cursorPosition++

                            KEY_BACKSPACE, 127/*, KEY_DELETE*/ -> {
                                if (cursorPosition - x > 0) {
                                    inputBuffer.deleteAt(cursorPosition - x - 1)
                                    cursorPosition--

                                    mvdelch(y, cursorPosition)
                                }
                            }

                            '\n'.code -> {
                                val input = inputBuffer.toString()
                                inputBuffer.clear()
                                cursorPosition = x
                                //do not just disable, maybe log lines want it back
                                curs_set(0)

                                inputMode = false
                                appState.holdCursor(false)

                                stringsSubject.emit(input)
                            }

                            //add ESC support

                            else -> {
                                val char = key.toChar()

                                if (char in ' '..'~') {
                                    inputBuffer.insert(cursorPosition - x, char)
                                    mvaddch(y, cursorPosition, key.toUInt())

                                    cursorPosition++
                                }
                            }
                        }
                        wmove(stdscr, y, cursorPosition)

                    } else {
                        Logger.d("${context()} Process key $key")
                        keypressesSubject.emit(key)
                    }
                }
            }
    }

    override suspend fun stop() {
        //will be stopped by CancellationException
    }
}
