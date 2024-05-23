/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.logLines

import com.norsedreki.dogcat.Dogcat
import com.norsedreki.dogcat.app.AppArguments
import com.norsedreki.dogcat.app.AppConfig.DEFAULT_TAG_WIDTH
import com.norsedreki.dogcat.app.AppState
import com.norsedreki.dogcat.app.Keymap
import com.norsedreki.dogcat.app.Keymap.Actions.END
import com.norsedreki.dogcat.app.Keymap.Actions.HOME
import com.norsedreki.dogcat.app.Keymap.Actions.LINE_DOWN
import com.norsedreki.dogcat.app.Keymap.Actions.LINE_UP
import com.norsedreki.dogcat.app.Keymap.Actions.PAGE_DOWN
import com.norsedreki.dogcat.app.Keymap.Actions.PAGE_UP
import com.norsedreki.dogcat.app.ui.HasLifecycle
import com.norsedreki.dogcat.app.ui.Input
import com.norsedreki.dogcat.state.DogcatState.Active
import com.norsedreki.dogcat.state.DogcatState.Inactive
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LogLinesPresenter(
    private val dogcat: Dogcat,
    private val appArguments: AppArguments,
    private val appState: AppState,
    private val input: Input
) : HasLifecycle {

    private lateinit var view: LogLinesView

    override suspend fun start() {
        view = LogLinesView()
        view.start()

        val scope = CoroutineScope(coroutineContext)

        scope.launch { collectAutoscroll() }
        scope.launch { collectAppState() }
        scope.launch { collectLogLines() }
        scope.launch { collectKeypresses() }
    }

    override suspend fun stop() {
        if (this::view.isInitialized) {
            view.stop()
        }
    }

    private suspend fun collectAutoscroll() {
        appState.state
            .map { it.autoscroll }
            .distinctUntilChanged()
            .collect {
                if (it) {
                    view.end()
                }
            }
    }

    private suspend fun collectAppState() {
        appState.state.collect {
            view.state =
                view.state.copy(
                    autoscroll = it.autoscroll,
                    showLineNumbers = appArguments.lineNumbers ?: false,
                    tagWidth = appArguments.tagWidth ?: DEFAULT_TAG_WIDTH,
                    isCursorHeld = it.isCursorHeld,
                    cursorReturnLocation = it.userInputLocation,
                    isUiHeld = it.isUiHeld,
                )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectLogLines() {
        dogcat.state
            .flatMapLatest {
                when (it) {
                    is Active -> {
                        Logger.d("${context()} Start capturing log lines")

                        view.state =
                            view.state.copy(
                                overscroll = false,
                            )

                        it.lines
                    }
                    Inactive -> {
                        Logger.d("${context()} Stop capturing log lines")
                        view.clear()

                        emptyFlow()
                    }
                }
            }
            .buffer(0)
            .collect { view.printLogLine(it) }
    }

    private suspend fun collectKeypresses() {
        input.keypresses.collect {
            when (Keymap.bindings[it]) {
                HOME -> {
                    appState.autoscroll(false)
                    view.home()
                }
                END -> {
                    appState.autoscroll(true)
                    view.end()
                }
                LINE_UP -> {
                    appState.autoscroll(false)
                    view.lineUp()
                }
                LINE_DOWN -> {
                    appState.autoscroll(false)
                    view.lineDown(1)
                }
                PAGE_DOWN -> {
                    appState.autoscroll(false)
                    view.pageDown()
                }
                PAGE_UP -> {
                    appState.autoscroll(false)
                    view.pageUp()
                }
                else -> {
                    // Other keys are handled elsewhere
                }
            }
        }
    }
}
