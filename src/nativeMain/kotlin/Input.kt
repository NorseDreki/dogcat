import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import ncurses.ERR
import ncurses.stdscr
import ncurses.wgetch

interface Input {
    val keypresses: Flow<Int>
}

class DefaultInput(
    private val scope: CoroutineScope,
    private val inputDispatcher: CoroutineDispatcher
) : Input {

    override val keypresses = MutableSharedFlow<Int>()

    val s = CoroutineScope(inputDispatcher)

    @OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
    fun start() {
        s.launch {
            while (true) {
                val key = wgetch(stdscr)

                if (key == ERR) { //!= EOF
                    delay(30)
                    continue
                }

                Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Process key $key")

                //debounce key presses
                keypresses.emit(key)
            }
        }
    }

    fun stop() {
        s.cancel()
    }
}
