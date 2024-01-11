package ui

import AppStateFlow
import Input
import logger.Logger
import dogcat.Command
import dogcat.Command.*
import dogcat.Dogcat
import dogcat.LogFilter.*
import dogcat.LogLevel.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logger.context
import ncurses.endwin
import platform.posix.exit

@OptIn(ExperimentalForeignApi::class)
class DogcatPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val scope: CoroutineScope
) {
    private val view = DogcatView()

    @OptIn(ExperimentalStdlibApi::class)
    fun start() {
        input
            .keypresses
            //.debounce(200)
            .onEach {
                when (it) {
                    'p'.code -> {
                        appStateFlow.autoscroll(!appStateFlow.state.value.autoscroll)
                    }
                    'q'.code -> { // catch control-c
                        dogcat(Command.Stop)
                        //pad.terminate()
                        //pad2.terminate()
                        endwin()
                        //resetty()
                        exit(0)
                    }

                    'c'.code -> {
                        dogcat(ClearLogSource)
                    }

                    '3'.code -> {
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

                    '4'.code -> {
                        dogcat(ResetFilter(Substring::class))
                    }

                    '5'.code -> {
                        dogcat(ResetFilter(MinLogLevel::class))
                    }

                    '6'.code -> {
                        dogcat(FilterBy(MinLogLevel(V)))
                    }

                    '7'.code -> {
                        dogcat(FilterBy(MinLogLevel(D)))
                    }

                    '8'.code -> {
                        dogcat(FilterBy(MinLogLevel(I)))
                    }

                    '9'.code -> {
                        dogcat(FilterBy(MinLogLevel(W)))
                    }

                    '0'.code -> {
                        dogcat(FilterBy(MinLogLevel(E)))
                    }
                }
            }
            .launchIn(scope)

        view.start()
    }
}
