package ui

import AppState
import com.norsedreki.dogcat.Command.*
import com.norsedreki.dogcat.Command.Start.*
import com.norsedreki.dogcat.Dogcat
import com.norsedreki.dogcat.LogFilter.*
import com.norsedreki.dogcat.LogLevel.*
import com.norsedreki.dogcat.state.DogcatState.Active
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ui.logLines.LogLinesPresenter
import ui.status.StatusPresenter
import userInput.Arguments
import userInput.Input
import userInput.Keymap
import userInput.Keymap.Actions.*
import kotlin.coroutines.coroutineContext

class AppPresenter(
    private val dogcat: Dogcat,
    private val arguments: Arguments,
    private val appState: AppState,
    private val input: Input,
    private val logLinesPresenter: LogLinesPresenter,
    private val statusPresenter: StatusPresenter,
) : HasLifecycle {

    private val view = AppView()

    override suspend fun start() {
        when {
            arguments.packageName != null -> dogcat(PickAppPackage(arguments.packageName!!))
            arguments.current == true -> dogcat(PickForegroundApp)
            else -> dogcat(PickAllApps)
        }

        view.start()

        val scope = CoroutineScope(coroutineContext)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectKeypresses() {
        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.device.isOnline }

            .catch {  } //!!

            .filter { it }
            .distinctUntilChanged()
            .flatMapLatest {
                input.keypresses
            }
            .onEach {
                handleKeypress(it)
            }
            //yet, catch terminates the chain
            /*.catch {
                Logger.d("CATCH IN collect keypresses")
            }
            .onCompletion {
                Logger.d("CATCH and complete")
            }*/
            .collect {
            }
    }

    private suspend fun handleKeypress(keyCode: Int) {
        when (Keymap.bindings[keyCode]) {

            AUTOSCROLL -> {
                appState.autoscroll(!appState.state.value.autoscroll)
            }

            CLEAR_LOGS -> {
                dogcat(ClearLogs)
            }

            TOGGLE_FILTER_BY_PACKAGE -> {
                val f = appState.state.value.packageFilter

                if (f.second) {
                    Logger.d("${context()} !DeselectSelectAppByPackage")
                    appState.filterByPackage(f.first, false)
                    dogcat(ResetFilter(ByPackage::class))
                } else if (f.first != null) {
                    Logger.d("${context()} !SelectAppByPackage")
                    dogcat(PickAppPackage(f.first!!.packageName))
                    appState.filterByPackage(f.first, true)
                }
            }

            RESET_FILTER_BY_SUBSTRING -> {
                dogcat(ResetFilter(Substring::class))
            }

            RESET_FILTER_BY_MIN_LOG_LEVEL -> {
                dogcat(ResetFilter(MinLogLevel::class))
            }

            MIN_LOG_LEVEL_V -> {
                dogcat(FilterBy(MinLogLevel(V)))
            }

            MIN_LOG_LEVEL_D -> {
                dogcat(FilterBy(MinLogLevel(D)))
            }

            MIN_LOG_LEVEL_I -> {
                dogcat(FilterBy(MinLogLevel(I)))
            }

            MIN_LOG_LEVEL_W -> {
                dogcat(FilterBy(MinLogLevel(W)))
            }

            MIN_LOG_LEVEL_E -> {
                dogcat(FilterBy(MinLogLevel(E)))
            }

            else -> {}
        }
    }
}
