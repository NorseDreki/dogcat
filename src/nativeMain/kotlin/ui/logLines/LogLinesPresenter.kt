package ui.logLines

import AppStateFlow
import Input
import Keymap.Actions.*
import logger.Logger
import dogcat.Dogcat
import dogcat.state.PublicState.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class LogLinesPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val scope: CoroutineScope,
    private val ui: CloseableCoroutineDispatcher
) {
    //views can come and go, when input disappears
    private val view = LogLinesView()

    @OptIn(ExperimentalForeignApi::class, ExperimentalCoroutinesApi::class)
    suspend fun start() {
        //what is tail-call as in launchIn?

        dogcat
            .state
            .flatMapLatest {
                when (it) {
                    is WaitingInput -> {
                        Logger.d("Waiting for log lines...\r")

                        emptyFlow()
                    }
                    is CapturingInput -> {
                        Logger.d("Capturing input...\r")
                        //make sure no capturing happens after clearing
                        withContext(ui) {
                            view.clear()
                        }


                        it.lines
                    }
                    InputCleared -> {
                        Logger.d("Cleared Logcat and re-started\r")
/*
                        withContext(ui) {
                            view.clear()
                        }
*/
                        emptyFlow()
                    }
                    Stopped -> {
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
                    when (Keymap.bindings[it]) {

                        Home -> {
                            appStateFlow.autoscroll(false)
                            view.home()
                        }

                        End -> {
                            appStateFlow.autoscroll(true)
                            view.end()
                        }

                        LineUp -> {
                            view.lineUp()
                        }

                        LineDown -> {
                            view.lineDown()
                        }

                        PageDown -> {
                            view.pageDown()
                        }

                        PageUp -> {
                            view.pageUp()
                        }

                        else -> {
                            //
                        }
                    }
                }
            }
            .launchIn(scope)
    }

    fun stop() {
        view.stop()
    }
}
