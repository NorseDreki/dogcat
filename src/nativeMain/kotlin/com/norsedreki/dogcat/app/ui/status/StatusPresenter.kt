/*
 * SPDX-FileCopyrightText: Copyright 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.status

import com.norsedreki.dogcat.Command.FilterBy
import com.norsedreki.dogcat.Dogcat
import com.norsedreki.dogcat.LogFilter.ByPackage
import com.norsedreki.dogcat.LogFilter.Substring
import com.norsedreki.dogcat.app.AppState
import com.norsedreki.dogcat.app.ui.HasLifecycle
import com.norsedreki.dogcat.app.ui.Input
import com.norsedreki.dogcat.state.DogcatState.Active
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class StatusPresenter(
    private val dogcat: Dogcat,
    private val appState: AppState,
    private val input: Input,
) : HasLifecycle {

    private lateinit var view: StatusView

    override suspend fun start() {
        view = StatusView()
        view.start()

        val scope = CoroutineScope(coroutineContext)

        scope.launch {
            collectDogcatState()
        }
        scope.launch {
            collectUserStringInput()
        }
        scope.launch {
            collectAppState()
        }
        scope.launch {
            collectDeviceStatus()
        }
    }

    override suspend fun stop() {
        if (this::view.isInitialized) {
            view.stop()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectDogcatState() {
        dogcat
            .state
            .filterIsInstance<Active>()
            .mapLatest { it }
            .collect {
                val filters = it.filters.first()

                filters[ByPackage::class]?.let {
                    appState.filterByPackage(it as ByPackage, true)
                }

                view.state = view.state.copy(
                    filters = filters,
                    autoscroll = appState.state.value.autoscroll,
                    deviceLabel = it.device.label,
                    isDeviceOnline = it.device.isOnline.first(),
                )
            }
    }

    private suspend fun collectUserStringInput() {
        input.strings
            .collect {
                dogcat(FilterBy(Substring(it)))
            }
    }

    private suspend fun collectAppState() {
        appState
            .state
            .collect {
                val packageName =
                    if (it.packageFilter.second) {
                        it.packageFilter.first!!.packageName
                    } else {
                        ""
                    }

                view.state = view.state.copy(
                    packageName = packageName,
                    autoscroll = it.autoscroll,
                    isCursorHeld = it.isCursorHeld,
                    cursorReturnLocation = it.userInputLocation,
                )
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectDeviceStatus() {
        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.device.isOnline }
            .distinctUntilChanged()
            .collect {
                view.state = view.state.copy(isDeviceOnline = it)
            }
    }
}
