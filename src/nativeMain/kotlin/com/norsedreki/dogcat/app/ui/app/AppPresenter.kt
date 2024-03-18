package com.norsedreki.dogcat.app.ui.app

import com.norsedreki.dogcat.app.AppState
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
import com.norsedreki.dogcat.app.ui.logLines.LogLinesPresenter
import com.norsedreki.dogcat.app.ui.status.StatusPresenter
import com.norsedreki.dogcat.app.AppArguments
import com.norsedreki.dogcat.app.Keymap
import com.norsedreki.dogcat.app.Keymap.Actions.*
import com.norsedreki.dogcat.app.ui.HasLifecycle
import com.norsedreki.dogcat.app.ui.Input
import kotlin.coroutines.coroutineContext

class AppPresenter(
    private val dogcat: Dogcat,
    private val appArguments: AppArguments,
    private val appState: AppState,
    private val input: Input,
    private val logLinesPresenter: LogLinesPresenter,
    private val statusPresenter: StatusPresenter,
) : HasLifecycle {

    private lateinit var view: AppView

    override suspend fun start() {
        when {
            appArguments.packageName != null -> {
                println("Resolving app package...")

                dogcat(PickAppPackage(appArguments.packageName!!))
            }

            appArguments.current == true -> {
                println("Resolving foreground app...")

                dogcat(PickForegroundApp)
            }

            else -> dogcat(PickAllApps)
        }

        view = AppView()
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

        if (this::view.isInitialized) {
            view.stop()
        }
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
                    val packageName = packageFilter.first!!.packageName
                    val appId = packageFilter.first!!.appId

                    dogcat(FilterBy(ByPackage(packageName, appId)))

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