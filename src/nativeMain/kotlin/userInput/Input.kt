package userInput

import AppConfig.INPUT_KEY_DELAY_MILLIS
import AppState
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ncurses.*
import ui.HasLifecycle
import ui.Strings.INPUT_FILTER_PREFIX
import userInput.Keymap.Actions.INPUT_FILTER_BY_SUBSTRING
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

        CoroutineScope(coroutineContext)
            .launch {
                val x = INPUT_FILTER_PREFIX.length
                val y = getmaxy(stdscr) - 1

                appState.setUserInputLocation(x, y)
                var cursorPosition = x
                mvwprintw(stdscr, y, 0, INPUT_FILTER_PREFIX)

                while (isActive) {
                    val key = wgetch(stdscr)

                    if (key == ERR) {
                        delay(INPUT_KEY_DELAY_MILLIS)

                        continue
                    }

                    if (Keymap.bindings[key] == INPUT_FILTER_BY_SUBSTRING && !inputMode) {
                        appState.holdCursor(true)
                        inputMode = true

                        wmove(stdscr, y, x)
                        wclrtoeol(stdscr)

                        wmove(stdscr, y, cursorPosition)
                        curs_set(1)
                        wrefresh(stdscr)

                        continue
                    }

                    // limit max input
                    if (inputMode) {
                        when (key) {
                            KEY_LEFT -> {
                                if (cursorPosition - x > 0) cursorPosition--
                            }
                            KEY_RIGHT -> {
                                if (cursorPosition - x < inputBuffer.length) cursorPosition++
                            }
                            KEY_BACKSPACE, 127 -> {
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
                                curs_set(0)

                                inputMode = false
                                appState.holdCursor(false)

                                stringsSubject.emit(input)
                            }
                            27 -> { // ESCAPE
                                mvwprintw(stdscr, y, x, " ".repeat(inputBuffer.length))

                                inputBuffer.clear()
                                cursorPosition = x
                            }

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
                        appState.setUserInputLocation(cursorPosition, y)

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
