/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.ui.help

import com.norsedreki.dogcat.app.ui.HasLifecycle
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope

class HelpPresenter : HasLifecycle {

    private lateinit var view: HelpView

    override suspend fun start() {
        view = HelpView()
        view.start()

        view.state = HelpView.State("")

        val scope = CoroutineScope(coroutineContext)

        //        scope.launch { collectAutoscroll() }
    }

    override suspend fun stop() {
        if (this::view.isInitialized) {
            view.stop()
        }
    }
}
