package ui

import AppStateFlow
import Input
import dogcat.Command
import dogcat.Dogcat
import dogcat.LogFilter
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ncurses.*
import platform.posix.*
import ui.status.StatusView

@OptIn(ExperimentalForeignApi::class)
class DogcatPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    val pkg: String? = null,
    private val scope: CoroutineScope
) {
    private val view = DogcatView()

    var isPackageFilteringEnabled = pkg != null

    fun start() {
        input
            .keypresses
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

                        dogcat(Command.ClearLogSource)
                    }



                    '3'.code -> {
                        isPackageFilteringEnabled =
                            if (isPackageFilteringEnabled) {
                                dogcat(Command.ResetFilter(LogFilter.ByPackage::class))
                                false
                            } else {
                                dogcat(Command.Start.SelectAppByPackage(pkg!!))
                                true
                            }
                    }

                    '4'.code -> {
                        dogcat(Command.ResetFilter(LogFilter.Substring::class))
                    }

                    '5'.code -> {
                        dogcat(Command.ResetFilter(LogFilter.MinLogLevel::class))
                    }

                    '6'.code -> {
                        dogcat(Command.FilterBy(LogFilter.MinLogLevel("V")))
                    }

                    '7'.code -> {
                        dogcat(Command.FilterBy(LogFilter.MinLogLevel("D")))
                    }

                    '8'.code -> {
                        dogcat(Command.FilterBy(LogFilter.MinLogLevel("I")))
                    }

                    '9'.code -> {
                        dogcat(Command.FilterBy(LogFilter.MinLogLevel("W")))
                    }

                    '0'.code -> {
                        dogcat(Command.FilterBy(LogFilter.MinLogLevel("E")))
                    }


                }
            }
            .launchIn(scope)

        view.start()
    }
}
