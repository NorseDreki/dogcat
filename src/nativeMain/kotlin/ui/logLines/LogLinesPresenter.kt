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
import kotlinx.coroutines.flow.*
import logger.context
import ui.HasLifecycle
import userInput.Keymap
import kotlin.coroutines.coroutineContext

class LogLinesPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val uiDispatcher: CoroutineDispatcher
) : HasLifecycle {
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

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalForeignApi::class)
    private suspend fun collectLogLines() {

        var i = 0
        dogcat
            .state
            .flatMapLatest {
                when (it) {
                    is Active -> {
                        Logger.d("${context()} Capturing input...")
                        i = 0
                        //make sure no capturing happens after clearing
                        withContext(uiDispatcher) {
                            //view.autoscroll = false
                            view.clear()
                        }

                        val waiting = "--------- log lines are empty, let's wait -- capturing"

                        withContext(uiDispatcher) {
                        //    view.processLogLine(IndexedValue(0, Unparseable(waiting)))
                        }

                        Logger.d("${context()} capturing input in pres ${it.lines}")

                        /*it.lines.take(50).collect {
                            Logger.d("${it.index} ${it.value}")
                        }*/



                        it.lines//.windowed(500.milliseconds)//.dropWhile { (it.value as LogLine).message == "" }
                    }

                    Inactive -> {
                        Logger.d("${context()} Cleared Logcat and re-started\r")
                        /*
                                                withContext(ui) {
                                                    view.clear()
                                                }
                        */
                        val waiting = "--------- log lines are empty, let's wait -- input cleared"

                        withContext(uiDispatcher) {
                            view.processLogLine(IndexedValue(0, Unparseable(waiting)))
                        }

                        emptyFlow()
                    }

                    Terminated -> {
                        Logger.d("${context()} No more reading lines, terminated\r")
                        emptyFlow()
                    }
                }
            }
            .buffer(0) //omg!
            .collect {
                withContext(uiDispatcher) {
                    //it.forEach {
                        if (i < 70) {
                            Logger.d("${it.index} ll")

                            i++
                        }

                        view.processLogLine(it)
                    //}
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
                        view.autoscroll = false
                        view.home()
                    }
                    End -> {
                        appStateFlow.autoscroll(true)
                        view.autoscroll = true
                        view.end()
                    }
                    LineUp -> {
                        appStateFlow.autoscroll(false)
                        view.autoscroll = false
                        view.lineUp()
                    }
                    LineDown -> {
                        appStateFlow.autoscroll(false)
                        view.autoscroll = false //?
                        view.lineDown(1)
                    }
                    PageDown -> {
                        val a = appStateFlow.state.value.autoscroll
                        view.pageDown(a)
                    }
                    PageUp -> {
                        appStateFlow.autoscroll(false)
                        view.autoscroll = false
                        view.pageUp()
                    }
                    else -> {}
                }
            }
    }
}
