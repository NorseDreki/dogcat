/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.help

import com.norsedreki.dogcat.app.AppState
import com.norsedreki.dogcat.app.Keymap
import com.norsedreki.dogcat.app.Keymap.Actions.HELP
import com.norsedreki.dogcat.app.ui.HasLifecycle
import com.norsedreki.dogcat.app.ui.Input
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class HelpPresenter(
    private val input: Input,
    private val appState: AppState,
) : HasLifecycle {

    override suspend fun start() {
        val scope = CoroutineScope(coroutineContext)

        scope.launch { collectKeypresses() }
    }

    override suspend fun stop() {
        // No op since views come and go along with hotkey for help.
    }

    private suspend fun collectKeypresses() {
        lateinit var view: HelpView

        var showing = false

        input.keypresses.collect { key ->
            when (Keymap.bindings[key]) {
                HELP -> {
                    val h = Keymap.bindings.entries.map { "${it.value.name} -- '${Char(it.key)}'" }

                    if (!showing) {
                        appState.holdUi(true)

                        view = HelpView()
                        view.start()

                        yield()
                        view.state = HelpView.State(h)

                        showing = true
                    } else {
                        showing = false
                        view.stop()

                        appState.holdUi(false)
                    }
                }
                else -> {
                    // Other keys are handled elsewhere
                }
            }
        }
    }
}
