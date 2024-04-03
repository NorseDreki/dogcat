/*
 * SPDX-FileCopyrightText: Copyright (C) 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app.di

import com.norsedreki.dogcat.Dogcat
import com.norsedreki.dogcat.LogLineBriefParser
import com.norsedreki.dogcat.LogLineParser
import com.norsedreki.dogcat.LogLines
import com.norsedreki.dogcat.Shell
import com.norsedreki.dogcat.app.AdbShell
import com.norsedreki.dogcat.state.DefaultLogFiltersState
import com.norsedreki.dogcat.state.LogFiltersState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

object DogcatModule {

    internal val dogcatModule =
        DI.Module("dogcat") {
            bindSingleton<LogFiltersState> { DefaultLogFiltersState() }
            bindSingleton<LogLineParser> { LogLineBriefParser() }
            bindSingleton<LogLines> {
                LogLines(
                    instance(),
                    instance(),
                    instance(),
                    Dispatchers.Default,
                )
            }
            bindSingleton<Shell> { AdbShell(Dispatchers.IO) }
            bindSingleton<Dogcat> {
                Dogcat(
                    instance(),
                    instance(),
                    instance(),
                )
            }
        }
}
