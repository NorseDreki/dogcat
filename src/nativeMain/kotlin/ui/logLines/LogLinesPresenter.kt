package ui.logLines

import AppStateFlow
import Input
import dogcat.Dogcat
import dogcat.PublicState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ncurses.*
import ui.ViewPosition

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
class LogLinesPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val scope: CoroutineScope,
    private val ui: CloseableCoroutineDispatcher
) {
    //decouple presentation from views
    private val sx = getmaxx(stdscr)
    private val sy = getmaxy(stdscr)

    private val viewPosition = ViewPosition(0, 0, sx, sy - 4) //- 5)

    //views can come and go, when input disappears
    private val view = LogLinesView(viewPosition)

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalForeignApi::class)
    suspend fun start() {
        //what is tail-call as in launchIn?

        dogcat
            .state
            .flatMapLatest {
                when (it) {
                    is PublicState.WaitingInput -> {
                        Logger.d("Waiting for log lines...\r")

                        emptyFlow()
                    }
                    is PublicState.CapturingInput -> {
                        it.lines//.take(10)
                    }
                    PublicState.InputCleared -> {
                        Logger.d("Cleared Logcat and re-started\r")
                        view.clear()

                        emptyFlow()
                    }
                    PublicState.Stopped -> {
                        //cancel()
                        Logger.d("No more reading lines, terminated\r")
                        emptyFlow()
                    }
                }
            }
            .onEach {
                withContext(ui) {
                    view.processLogLine(it)
                }
            }
            .launchIn(scope)

        input
            .keypresses
            .onEach {
                withContext(ui) {
                    when (it) {
                        'a'.code, KEY_HOME -> {
                            appStateFlow.autoscroll(false)
                            view.home()
                        }

                        'z'.code, KEY_END -> {
                            appStateFlow.autoscroll(true)
                            view.end()
                        }

                        'w'.code, KEY_UP -> {
                            Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Key up")
                            view.lineUp()
                        }

                        's'.code, KEY_DOWN -> view.lineDown()

                        'd'.code, KEY_NPAGE -> view.pageDown()

                        'e'.code, KEY_PPAGE -> view.pageUp()
                    }
                }
            }
            .launchIn(scope)
    }

    fun stop() {
        view.stop()
    }
}
