package ui

import AppStateFlow
import userInput.Input
import userInput.Keymap.Actions.*
import logger.Logger
import dogcat.Command
import dogcat.Command.*
import dogcat.Dogcat
import dogcat.LogFilter.*
import dogcat.LogLevel.*
import dogcat.state.PublicState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logger.context
import ncurses.endwin
import platform.posix.exit
import userInput.Keymap

@OptIn(ExperimentalForeignApi::class)
class AppPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val scope: CoroutineScope
) {
    private val view = AppView()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        dogcat
            .state
            .flatMapLatest {
                when (it) {
                    is PublicState.WaitingInput -> {
                        Logger.d("Waiting for log lines...\r")

                        emptyFlow()
                    }
                    is PublicState.CapturingInput -> {
                        it.lines
                    }
                    PublicState.InputCleared -> {
                        Logger.d("Cleared Logcat and re-started\r")
                        /*
                                                withContext(ui) {
                                                    view.clear()
                                                }
                        */
                        emptyFlow()
                    }
                    PublicState.Stopped -> {
                        // or maybe SDK tools are not installed at all
                        println("Either ADB is not found in your PATH or it's found but no emulator is running " +
                                "(it may be disconnected or stopped). Quitting.\n")

                        emptyFlow()
                    }
                }
            }
            .onEach {
            }
            .launchIn(scope)


        input
            .keypresses
            //.debounce(200)
            .onEach {
                when (Keymap.bindings[it]) {

                    Autoscroll -> {
                        appStateFlow.autoscroll(!appStateFlow.state.value.autoscroll)
                    }

                    Quit -> { // catch control-c
                        dogcat(Command.Stop)
                        //pad.terminate()
                        //pad2.terminate()
                        endwin()
                        //resetty()
                        exit(0)
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
            .launchIn(scope)

        view.start()
    }
}
