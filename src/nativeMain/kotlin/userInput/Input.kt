package userInput

import AppConfig.INPUT_KEY_DELAY_MILLIS
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
    private val ioDispatcher: CoroutineDispatcher
) : Input {

    private val keypressesSubject = MutableSharedFlow<Int>()
    override val keypresses = keypressesSubject     //.debounceRepetitiveKeys(150.milliseconds)

    private val stringsSubject = MutableSharedFlow<String>()
    override val strings = stringsSubject

    private var inputMode = false
    //private val inputModeMutex = Mutex()

    private val fl = 0//"Filter: ".length

    private var inputBuffer = StringBuilder()
    private var cursorPosition = fl

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun start() {
        CoroutineScope(coroutineContext)
            .launch {//(ioDispatcher) {
                while (isActive) {
                    val key = wgetch(stdscr)

                    if (key == ERR) {

                        if (inputMode) {
                            curs_set(1)
                            wmove(stdscr, 49 , cursorPosition)
                            wrefresh(stdscr)
                        }

                        delay(INPUT_KEY_DELAY_MILLIS)

                        continue
                    }

                    if (Keymap.bindings[key] == Keymap.Actions.InputFilterBySubstring) {
                        inputMode = true
                        //echo()

                        continue
                    }

                    if (inputMode) {
                        when (key) {
                            KEY_LEFT -> if (cursorPosition > 0) cursorPosition--
                            KEY_RIGHT -> if (cursorPosition < inputBuffer.length) cursorPosition++
                            KEY_BACKSPACE, 127/*, KEY_DELETE*/ -> {
                                Logger.d("${context()} Delete char")

                                if (cursorPosition > 0) {
                                    inputBuffer.deleteAt(cursorPosition - fl - 1)
                                    cursorPosition--
                                    // Delete the character on the screen
                                    mvdelch(49, cursorPosition)
                                }
                            }

                            '\n'.code -> {
                                // Handle the completed input
                                val input = inputBuffer.toString()
                                inputBuffer.clear()
                                cursorPosition = fl
                                //noecho()
                                inputMode = false
                                // Emit the input string
                                stringsSubject.emit(input)
                            }

                            else -> {
                                val char = key.toChar()
                                if (char in ' '..'~') {
                                    inputBuffer.insert(cursorPosition, char)
                                    mvaddch(49, cursorPosition, key.toUInt())
                                    cursorPosition++
                                    // Add the character to the screen
                                }
                            }
                        }
                        // Move the cursor to the correct position
                        wmove(stdscr, 49, cursorPosition)

                    } else {
                        Logger.d("${context()} Process key $key")
                        keypressesSubject.emit(key)
                    }
                }
            }


        /*CoroutineScope(coroutineContext)
            .launch(ioDispatcher) {
                while (isActive) {
                    val key = wgetch(stdscr)

                    if (key == ERR) {
                        delay(INPUT_KEY_DELAY_MILLIS)

                        continue
                    }
                    Logger.d("${context()} Process key $key")

                    keypressesSubject.emit(key)
                }
            }*/
    }

    override suspend fun stop() {
        //will be stopped by CancellationException
    }
}
