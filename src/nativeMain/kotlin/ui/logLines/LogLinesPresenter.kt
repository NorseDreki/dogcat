package ui.logLines

import AppState
import dogcat.Dogcat
import dogcat.Unparseable
import dogcat.state.PublicState.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import logger.Logger
import logger.context
import ui.HasLifecycle
import userInput.Arguments
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
                .map { it.autoscroll }
                .distinctUntilChanged()
                .collect {
                    if (it) {
                        view.end()
                    }
                }
        }

        scope.launch {
            appState.state
                .collect {
                    view.state = view.state.copy(
                        autoscroll = it.autoscroll,
                        showLineNumbers = Arguments.lineNumbers ?: false,
                        isCursorHeld = it.isCursorHeld,
                        cursorReturnLocation = it.inputFilterLocation
                    )
                }
        }
    }

    override suspend fun stop() {
        view.stop()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectLogLines() {
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
                view.processLogLine(it)
            }
    }

    private suspend fun collectKeypresses() {
        input
            .keypresses
            .collect {
                when (Keymap.bindings[it]) {
                    HOME -> {
                        appState.autoscroll(false)
                        view.home()
                    }

                    END -> {
                        appState.autoscroll(true)
                        view.end()
                    }

                    LINE_UP -> {
                        appState.autoscroll(false)
                        view.lineUp()
                    }

                    LINE_DOWN -> {
                        appState.autoscroll(false)
                        view.lineDown(1)
                    }

                    PAGE_DOWN -> {
                        appState.autoscroll(false)
                        view.pageDown()
                    }

                    PAGE_UP -> {
                        appState.autoscroll(false)
                        view.pageUp()
                    }

                    else -> {
                        // Other keys are handled elsewhere
                    }
                }
            }
    }
}
