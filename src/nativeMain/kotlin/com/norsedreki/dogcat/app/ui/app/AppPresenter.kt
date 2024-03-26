/*
 * SPDX-FileCopyrightText: Copyright 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.app

import com.norsedreki.dogcat.Command.ClearLogs
import com.norsedreki.dogcat.Command.FilterBy
import com.norsedreki.dogcat.Command.ResetFilter
import com.norsedreki.dogcat.Command.Start.PickAllApps
import com.norsedreki.dogcat.Command.Start.PickAppPackage
import com.norsedreki.dogcat.Command.Start.PickForegroundApp
import com.norsedreki.dogcat.Command.Stop
import com.norsedreki.dogcat.Dogcat
import com.norsedreki.dogcat.LogFilter.ByPackage
import com.norsedreki.dogcat.LogFilter.MinLogLevel
import com.norsedreki.dogcat.LogFilter.Substring
import com.norsedreki.dogcat.LogLevel.D
import com.norsedreki.dogcat.LogLevel.E
import com.norsedreki.dogcat.LogLevel.I
import com.norsedreki.dogcat.LogLevel.V
import com.norsedreki.dogcat.LogLevel.W
import com.norsedreki.dogcat.app.AppArguments
import com.norsedreki.dogcat.app.AppState
import com.norsedreki.dogcat.app.Keymap
import com.norsedreki.dogcat.app.Keymap.Actions.AUTOSCROLL
import com.norsedreki.dogcat.app.Keymap.Actions.CLEAR_LOGS
import com.norsedreki.dogcat.app.Keymap.Actions.MIN_LOG_LEVEL_D
import com.norsedreki.dogcat.app.Keymap.Actions.MIN_LOG_LEVEL_E
import com.norsedreki.dogcat.app.Keymap.Actions.MIN_LOG_LEVEL_I
import com.norsedreki.dogcat.app.Keymap.Actions.MIN_LOG_LEVEL_V
import com.norsedreki.dogcat.app.Keymap.Actions.MIN_LOG_LEVEL_W
import com.norsedreki.dogcat.app.Keymap.Actions.RESET_FILTER_BY_MIN_LOG_LEVEL
import com.norsedreki.dogcat.app.Keymap.Actions.RESET_FILTER_BY_SUBSTRING
import com.norsedreki.dogcat.app.Keymap.Actions.TOGGLE_FILTER_BY_PACKAGE
import com.norsedreki.dogcat.app.ui.HasLifecycle
import com.norsedreki.dogcat.app.ui.Input
import com.norsedreki.dogcat.app.ui.logLines.LogLinesPresenter
import com.norsedreki.dogcat.app.ui.status.StatusPresenter
import com.norsedreki.dogcat.state.DogcatState.Active
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

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
            }.collect {
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
