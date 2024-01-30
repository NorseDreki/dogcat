package userInput

import AppConfig.INPUT_KEY_DELAY_MILLIS
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import logger.Logger
import logger.context
import ncurses.ERR
import ncurses.stdscr
import ncurses.wgetch
import ui.HasLifecycle
import kotlin.coroutines.coroutineContext

interface Input : HasLifecycle {
    val keypresses: Flow<Int>
}

class DefaultInput(
    private val ioDispatcher: CoroutineDispatcher
) : Input {

    private val keypressesSubject = MutableSharedFlow<Int>()
    override val keypresses = keypressesSubject//.debounceRepetitiveKeys(150.milliseconds)

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun start() {
        CoroutineScope(coroutineContext)
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
            }
    }

    override suspend fun stop() {
        //will be stopped by CancellationException
    }
}
