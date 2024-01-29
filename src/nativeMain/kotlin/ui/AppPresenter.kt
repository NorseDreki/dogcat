package ui

import AppStateFlow
import dogcat.Command.*
import dogcat.Dogcat
import dogcat.LogFilter.*
import dogcat.LogLevel.*
import dogcat.state.PublicState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import logger.Logger
import logger.context
import ui.logLines.LogLinesPresenter
import ui.status.StatusPresenter
import userInput.Arguments
import userInput.HasHifecycle
import userInput.Input
import userInput.Keymap
import userInput.Keymap.Actions.*
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalForeignApi::class)
class AppPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val logLinesPresenter: LogLinesPresenter,
    private val statusPresenter: StatusPresenter,
) : HasHifecycle {
    private val view = AppView()

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun start() {
        view.start()

        val s = CoroutineScope(coroutineContext)

        s.launch {
            collectDogcatEvents()
        }
        s.launch {
            collectKeypresses()
        }

        when {
            Arguments.packageName != null -> dogcat(Start.PickApp(Arguments.packageName!!))
            Arguments.current == true -> dogcat(Start.PickForegroundApp)
            else -> dogcat(Start.All)
        }

        logLinesPresenter.start()
        //statusPresenter.start()
    }

    override suspend fun stop() {
        logLinesPresenter.stop()
        statusPresenter.stop()

        view.stop()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectDogcatEvents() {
        dogcat
            .state
            .filterIsInstance<PublicState.Stopped>()
            .collect {
                println(
                    "Either ADB is not found in your PATH or it's found but no emulator is running ")

            }
    }

    private suspend fun collectKeypresses() {
        input
            .keypresses
            .onEach {
                when (Keymap.bindings[it]) {
                    Autoscroll -> {
                        appStateFlow.autoscroll(!appStateFlow.state.value.autoscroll)
                    }
                    Quit -> { // catch control-c
                        //dogcat(Command.Stop)
                        //coroutineContext.cancelChildren()
                        //currentCoroutineContext().cancelChildren()
                        //scope.coroutineContext.cancelChildren() -- only this works
                        Logger.d("{${context()} ++++++ cancelled ${coroutineContext}'s job")
                        //endwin()

                        //pad.terminate()
                        //pad2.terminate()
                        //resetty()
                        //exit(0)
                    }

                    ClearLogs -> {
                        dogcat(ClearLogSource)
                    }

                    ToggleFilterByPackage -> {
                        val f = appStateFlow.state.value.packageFilter

                        if (f.second) {
                            Logger.d("${context()} !DeselectSelectAppByPackage")
                            appStateFlow.filterByPackage(f.first, false)
                            dogcat(ResetFilter(ByPackage::class))
                        } else {
                            Logger.d("${context()} !SelectAppByPackage")
                            dogcat(Start.PickApp(f.first!!.packageName))
                            appStateFlow.filterByPackage(f.first, true)
                        }
                    }

                    ResetFilterBySubstring -> {
                        dogcat(ResetFilter(Substring::class))
                    }

                    ResetFilterByMinLogLevel -> {
                        dogcat(ResetFilter(MinLogLevel::class))
                    }

                    MinLogLevelV -> {
                        dogcat(FilterBy(MinLogLevel(V)))
                    }

                    MinLogLevelD -> {
                        dogcat(FilterBy(MinLogLevel(D)))
                    }

                    MinLogLevelI -> {
                        dogcat(FilterBy(MinLogLevel(I)))
                    }

                    MinLogLevelW -> {
                        dogcat(FilterBy(MinLogLevel(W)))
                    }

                    MinLogLevelE -> {
                        dogcat(FilterBy(MinLogLevel(E)))
                    }

                    else -> {}
                }
            }
            .onCompletion {
                Logger.d("${context()} ++++++ no longer listening to key prese")
            }
            .collect()
    }
}
