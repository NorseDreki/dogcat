package ui

import AppStateFlow
import Input
import Keymap.Actions.*
import logger.Logger
import dogcat.Command
import dogcat.Command.*
import dogcat.Dogcat
import dogcat.LogFilter.*
import dogcat.LogLevel.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logger.context
import ncurses.endwin
import platform.posix.exit

@OptIn(ExperimentalForeignApi::class)
class AppPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val scope: CoroutineScope
) {
    private val view = AppView()

    fun start() {
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
