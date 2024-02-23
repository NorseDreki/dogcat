package ui

import AppStateFlow
import dogcat.Command.*
import dogcat.Dogcat
import dogcat.LogFilter.*
import dogcat.LogLevel.*
import dogcat.state.PublicState
import dogcat.state.PublicState.Active
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
import userInput.Input
import userInput.Keymap
import userInput.Keymap.Actions.*
import kotlin.coroutines.coroutineContext

class AppPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val logLinesPresenter: LogLinesPresenter,
    private val statusPresenter: StatusPresenter,
) : HasLifecycle {
    private val view = AppView()

    override suspend fun start() {
        when {
            Arguments.packageName != null -> dogcat(Start.PickAppPackage(Arguments.packageName!!))
            Arguments.current == true -> dogcat(Start.PickForegroundApp)
            else -> dogcat(Start.PickAllApps)
        }

        appStateFlow.setInputFilterLocation("Filter: ".length, 49)

        view.start()

        val scope = CoroutineScope(coroutineContext)
        scope.launch {
            collectDogcatEvents()
        }
        scope.launch {
            collectKeypresses()
        }

        logLinesPresenter.start()
        statusPresenter.start()
    }

    override suspend fun stop() {
        dogcat(Stop)

        logLinesPresenter.stop()
        statusPresenter.stop()

        view.stop()
    }

    private suspend fun collectDogcatEvents() {
        dogcat
            .state
            .filterIsInstance<PublicState.Terminated>()
            .collect {
                println(
                    "Either ADB is not found in your PATH or it's found but no emulator is running "
                )
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectKeypresses() {
        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.heartbeat }
            .filter { it }
            .distinctUntilChanged()
            .flatMapLatest {
                input.keypresses
            }
            //catch?
            .collect {
                handleKeypress(it)
            }
        /*input
            .keypresses
            .onEach {
            }
            .catch {
                //we need to catch dogcat exception here

                Logger.d("${context()} [[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ catch on input chain $it")
            }
            .onCompletion {
                Logger.d("${context()} ++++++ no longer listening to key prese")
            }
            .collect()*/
    }

    private suspend fun handleKeypress(keyCode: Int) {
        when (Keymap.bindings[keyCode]) {

            Autoscroll -> {
                appStateFlow.autoscroll(!appStateFlow.state.value.autoscroll)
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
                } else if (f.first != null) {
                    Logger.d("${context()} !SelectAppByPackage")
                    dogcat(Start.PickAppPackage(f.first!!.packageName))
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
}
