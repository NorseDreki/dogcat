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

interface Input : HasHifecycle {
    val keypresses: Flow<Int>
}

interface HasHifecycle {

    fun start()

    fun stop()
}

class DefaultInput(
    private val scope: CoroutineScope,
    private val inputDispatcher: CoroutineDispatcher
) : Input {

    private val keypressesSubject = MutableSharedFlow<Int>()

    @OptIn(FlowPreview::class)
    // debounce somewhere else?
    override val keypresses = keypressesSubject//.debounce(300)

    val s = CoroutineScope(inputDispatcher)

    @OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
    override fun start() {
        s.launch {
            while (true) {
                val key = wgetch(stdscr)

                if (key == ERR) { //!= EOF
                    delay(30)
                    continue
                }

                Logger.d("${context()} Process key $key")

                //debounce key presses
                keypressesSubject.emit(key)
            }
        }
    }

    override fun stop() {
        s.cancel()
    }
}
