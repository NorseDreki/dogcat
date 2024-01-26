package ui.logLines

import AppStateFlow
import userInput.Input
import userInput.Keymap.Actions.*
import logger.Logger
import dogcat.Dogcat
import dogcat.Unparseable
import dogcat.state.PublicState.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import logger.context
import userInput.HasHifecycle
import userInput.Keymap
import kotlin.coroutines.coroutineContext

class LogLinesPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val uiDispatcher: CoroutineDispatcher
) : HasHifecycle {
    //views can come and go, when input disappears
    private lateinit var view: LogLinesView

    @OptIn(ExperimentalForeignApi::class, ExperimentalCoroutinesApi::class)
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
                    is WaitingInput -> {
                        Logger.d("${context()} Waiting for log lines...\r")
                        val waiting = "--------- log lines are empty, let's wait"

                        withContext(uiDispatcher) {
                            view.processLogLine(IndexedValue(0, Unparseable(waiting)))
                        }
                        emptyFlow()
                    }

                    is CapturingInput -> {
                        Logger.d("${context()} Capturing input...\r")
                        //make sure no capturing happens after clearing
                        withContext(uiDispatcher) {
                            view.clear()
                        }

                        val waiting = "--------- log lines are empty, let's wait"

                        withContext(uiDispatcher) {
                            view.processLogLine(IndexedValue(0, Unparseable(waiting)))
                        }

                        it.lines
                    }

                    InputCleared -> {
                        Logger.d("${context()} Cleared Logcat and re-started\r")
                        /*
                                                withContext(ui) {
                                                    view.clear()
                                                }
                        */
                        val waiting = "--------- log lines are empty, let's wait"

                        withContext(uiDispatcher) {
                            view.processLogLine(IndexedValue(0, Unparseable(waiting)))
                        }

                        emptyFlow()
                    }

                    Stopped -> {
                        Logger.d("${context()} No more reading lines, terminated\r")
                        emptyFlow()
                    }
                }
            }
            .collect {
                withContext(uiDispatcher) {
                    view.processLogLine(it)
                }
            }
    }

    private suspend fun collectKeypresses() {
        input
            .keypresses
            .collect {
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
                        appStateFlow.autoscroll(false)
                        view.lineUp()
                    }
                    LineDown -> {
                        view.lineDown()
                    }
                    PageDown -> {
                        val a = appStateFlow.state.value.autoscroll
                        view.pageDown(a)
                    }
                    PageUp -> {
                        appStateFlow.autoscroll(false)
                        view.pageUp()
                    }
                    else -> {}
                }
            }
    }
}
