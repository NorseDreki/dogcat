package ui

import AppState
import com.norsedreki.dogcat.Command.*
import com.norsedreki.dogcat.Command.Start.*
import com.norsedreki.dogcat.Dogcat
import com.norsedreki.dogcat.LogFilter.*
import com.norsedreki.dogcat.LogLevel.*
import com.norsedreki.dogcat.state.DogcatState.Active
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ui.logLines.LogLinesPresenter
import ui.status.StatusPresenter
import userInput.AppArguments
import userInput.Input
import userInput.Keymap
import userInput.Keymap.Actions.*
import kotlin.coroutines.coroutineContext

class AppPresenter(
    private val dogcat: Dogcat,
    private val appArguments: AppArguments,
    private val appState: AppState,
    private val input: Input,
    private val logLinesPresenter: LogLinesPresenter,
    private val statusPresenter: StatusPresenter,
) : HasLifecycle {

    private val view = AppView()

    override suspend fun start() {
        when {
            appArguments.packageName != null -> dogcat(PickAppPackage(appArguments.packageName!!))
            appArguments.current == true -> dogcat(PickForegroundApp)
            else -> dogcat(PickAllApps)
        }

        view.start()

        logLinesPresenter.start()
        statusPresenter.start()

        val scope = CoroutineScope(coroutineContext)
        scope.launch {
            collectKeypresses()
        }
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
            .distinctUntilChanged()
            .flatMapLatest { isOnline ->
                if (isOnline) {
                    input.keypresses
                } else {
                    emptyFlow()
                }
            }
            .collect {
                dispatchKeyCode(it)
            }
    }

    private suspend fun dispatchKeyCode(keyCode: Int) {
        when (Keymap.bindings[keyCode]) {

            AUTOSCROLL -> {
                val currentAutoscroll = appState.state.value.autoscroll
                appState.autoscroll(!currentAutoscroll)
            }

            CLEAR_LOGS -> {
                dogcat(ClearLogs)
            }

            TOGGLE_FILTER_BY_PACKAGE -> {
                val packageFilter = appState.state.value.packageFilter

                if (packageFilter.second) {
                    appState.filterByPackage(packageFilter.first, false)
                    dogcat(ResetFilter(ByPackage::class))

                } else if (packageFilter.first != null) {
                    dogcat(PickAppPackage(packageFilter.first!!.packageName))
                    appState.filterByPackage(packageFilter.first, true)
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

            else -> {
                // Other keys are handled elsewhere
            }
        }
    }
}
