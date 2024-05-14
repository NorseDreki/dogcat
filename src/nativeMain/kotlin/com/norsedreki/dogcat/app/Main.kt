/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app

import com.norsedreki.dogcat.app.AppArguments.ValidationException
import com.norsedreki.dogcat.app.AppConfig.EXIT_CODE_ERROR
import com.norsedreki.dogcat.app.Keymap.Actions.QUIT
import com.norsedreki.dogcat.app.di.AppModule
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import platform.posix.exit

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    var exitCode = EXIT_CODE_ERROR

    val appModule = AppModule()
    Logger.set(appModule.fileLogger)

    try {
        appModule.appArguments.validate(args)
    } catch (e: ValidationException) {
        println(e.message)

        Logger.d("Arguments validation failed: ${e.message}")

        exit(exitCode)
    }

    if (appModule.appArguments.version == true) {
        // val c = '\u2026'
        val c = '\u2026'

        val m =
            """
            ${c}Dogcat version ${BuildConfig.VERSION} by Alex Dmitriev ()

            A terminal-based Android Logcat reader with sane colouring
            https://github.com/NorseDreki/dogcat
            """
                .trimIndent()

        println(m)
        exit(0)
    }

    if (appModule.appArguments.printKeymap == true) {
        Keymap.bindings.entries.forEach { println("${it.value.name} -- '${Char(it.key)}'") }

        exit(0)
    }

    val handler = CoroutineExceptionHandler { _, e ->
        Logger.d("Exception in top-level handler: ${e.message}")

        runBlocking { appModule.appPresenter.stop() }

        val causeMessage = e.cause?.message
        val cause = ", cause: $causeMessage"

        val message =
            if (causeMessage != null) {
                e.message + cause
            } else {
                e.message
            }

        println(message)
    }

    val ui = newSingleThreadContext("UI")

    runBlocking(ui) {
        val appJob =
            CoroutineScope(ui).launch(handler) {
                appModule.appPresenter.start()
                appModule.input.start()

                appModule.input.keypresses
                    .filter { Keymap.bindings[it] == QUIT }
                    .onEach {
                        Logger.d("${context()} User quits the application")

                        appModule.appPresenter.stop()
                        coroutineContext.cancelChildren()

                        exitCode = 0
                    }
                    .launchIn(this@launch)
            }
        appJob.join()
    }

    ui.close()

    Logger.d("Exiting the application with code $exitCode")
    Logger.close()

    exit(exitCode)
}
