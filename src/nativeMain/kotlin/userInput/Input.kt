package userInput

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import logger.Logger
import logger.context
import ncurses.ERR
import ncurses.stdscr
import ncurses.wgetch
import kotlin.coroutines.coroutineContext

interface Input : HasHifecycle {
    val keypresses: Flow<Int>
}

interface HasHifecycle {
    suspend fun start()

    suspend fun stop()
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
                        delay(30)
                        continue
                    }
                    Logger.d("${context()} Process key $key")

                    keypressesSubject.emit(key)
                }
            }
    }

    override suspend fun stop() {
        //not expected to be called
    }
}
