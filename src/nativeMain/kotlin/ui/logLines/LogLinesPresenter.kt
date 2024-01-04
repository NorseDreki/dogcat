package ui.logLines

import ui.status.StatusView

import AppStateFlow
import Input
import dogcat.Command
import dogcat.Dogcat
import dogcat.LogFilter
import dogcat.PublicState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ncurses.*

@OptIn(ExperimentalForeignApi::class)
class LogLinesPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val scope: CoroutineScope
) {
    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)

    val padPosition = PadPosition(0, 0, sx, sy - 5)

    //views can come and go, when input disappears
    private val view = LogLinesView(padPosition)

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
            .onEach { view.processLogLine(it) }
            .launchIn(scope)

        input
            .keypresses
            .onEach {
                when (it) {

                    'a'.code, KEY_HOME -> {
                        appStateFlow.autoscroll(false)
                        view.home()
                    }

                    'z'.code, KEY_END -> {
                        appStateFlow.autoscroll(true)
                        view.end()
                    }

                    'w'.code, KEY_UP -> view.lineUp()

                    's'.code, KEY_DOWN -> view.lineDown()

                    'd'.code, KEY_NPAGE -> view.pageDown()

                    'e'.code, KEY_PPAGE -> view.pageUp()

                }
            }
            .launchIn(scope)

        appStateFlow
            .state
            .onEach {

            }
            .launchIn(scope)
    }

    suspend fun stop() {
        //view.stop()
    }
}
