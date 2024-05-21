/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.di

import com.norsedreki.dogcat.app.AppArguments
import com.norsedreki.dogcat.app.AppState
import com.norsedreki.dogcat.app.BuildConfig
import com.norsedreki.dogcat.app.FileLogger
import com.norsedreki.dogcat.app.InternalAppState
import com.norsedreki.dogcat.app.di.DogcatModule.dogcat
import com.norsedreki.dogcat.app.ui.DefaultInput
import com.norsedreki.dogcat.app.ui.Input
import com.norsedreki.dogcat.app.ui.app.AppPresenter
import com.norsedreki.dogcat.app.ui.logLines.LogLinesPresenter
import com.norsedreki.dogcat.app.ui.status.StatusPresenter
import com.norsedreki.logger.CanLog
import com.norsedreki.logger.NoOpLogger
import kotlinx.cli.ArgParser
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

class AppModule {

    private val app =
        DI.Module("app") {
            bindSingleton<CanLog> {
                if (BuildConfig.DEBUG) {
                    FileLogger()
                } else {
                    NoOpLogger()
                }
            }
            bindSingleton<AppArguments> { AppArguments(ArgParser("dogcat")) }
            bindSingleton<AppState> { InternalAppState() }
            bindSingleton<Input> { DefaultInput(instance()) }
            bindSingleton<AppPresenter> {
                AppPresenter(
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                )
            }
            bindSingleton<StatusPresenter> {
                StatusPresenter(
                    instance(),
                    instance(),
                    instance(),
                )
            }
            bindSingleton<LogLinesPresenter> {
                LogLinesPresenter(
                    instance(),
                    instance(),
                    instance(),
                    instance(),
                )
            }
        }

    private val serviceLocator = DI {
        import(dogcat)
        import(app)
    }

    val fileLogger: CanLog by serviceLocator.instance()

    val appArguments: AppArguments by serviceLocator.instance()

    val input: Input by serviceLocator.instance()

    val appPresenter: AppPresenter by serviceLocator.instance()
}
