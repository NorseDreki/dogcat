package ui.logLines

import AppState
import dogcat.Dogcat
import dogcat.Unparseable
import dogcat.state.PublicState.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import logger.Logger
import logger.context
import ui.HasLifecycle
import userInput.Input
import userInput.Keymap
import userInput.Keymap.Actions.*
import kotlin.coroutines.coroutineContext

class LogLinesPresenter(
    private val dogcat: Dogcat,
    private val appState: AppState,
    private val input: Input,
) : HasLifecycle {
    //views can come and go, when input disappears
    private lateinit var view: LogLinesView

    override suspend fun start() {
        view = LogLinesView()
        //view.start()

        val scope = CoroutineScope(coroutineContext)

        scope.launch {
            collectLogLines()
        }
        scope.launch {
            collectKeypresses()
        }

        scope.launch {
            appState.state
                .map { it.isCursorHeld }
                .collect {
                    view.state = view.state.copy(
                        isCursorHeld = it
                    )
                }
        }
    }

    override suspend fun stop() {
        view.stop()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectLogLines() {
        var i = 0
        dogcat
            .state
            .flatMapLatest {
                when (it) {
                    is Active -> {
                        Logger.d("${context()} Capturing input...")
                        view.clear()

                        it.lines
                    }

                    Inactive -> {
                        Logger.d("${context()} Cleared Logcat and re-started")
                        val waiting = "--------- log lines are empty, let's wait -- input cleared"

                        view.processLogLine(IndexedValue(0, Unparseable(waiting)))

                        emptyFlow()
                    }

                    Terminated -> {
                        Logger.d("${context()} No more reading lines, terminated")
                        emptyFlow()
                    }
                }
            }
            .buffer(0) //omg!
            .collect {
                if (i < 20) {
                    Logger.d("${context()} ${it.index} ll")

                    i++
                }

                view.processLogLine(it)
            }
    }

    private suspend fun collectKeypresses() {
        input
            .keypresses
            .collect {
                Logger.d("${context()} Log lines key")
                when (Keymap.bindings[it]) {
                    Home -> {
                        appState.autoscroll(false)
                        view.state = view.state.copy(autoscroll = false)
                        view.home()
                    }

                    End -> {
                        appState.autoscroll(true)
                        view.state = view.state.copy(autoscroll = true)
                        view.end()
                    }

                    LineUp -> {
                        appState.autoscroll(false)
                        view.state = view.state.copy(autoscroll = false)
                        view.lineUp()
                    }

                    LineDown -> {
                        appState.autoscroll(false)
                        view.state = view.state.copy(autoscroll = false)
                        view.lineDown(1)
                    }

                    PageDown -> {
                        view.pageDown()
                    }

                    PageUp -> {
                        appState.autoscroll(false)
                        view.state = view.state.copy(autoscroll = false)
                        view.pageUp()
                    }

                    else -> {}
                }
            }
    }
}
