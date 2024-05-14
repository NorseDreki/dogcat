/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

import com.norsedreki.dogcat.Command.ClearLogs
import com.norsedreki.dogcat.Command.FilterBy
import com.norsedreki.dogcat.Command.ResetFilter
import com.norsedreki.dogcat.Command.Start
import com.norsedreki.dogcat.Command.Start.PickAllApps
import com.norsedreki.dogcat.Command.Start.PickAppPackage
import com.norsedreki.dogcat.Command.Start.PickForegroundApp
import com.norsedreki.dogcat.Command.Stop
import com.norsedreki.dogcat.LogFilter.ByPackage
import com.norsedreki.dogcat.LogFilter.Substring
import com.norsedreki.dogcat.state.Device
import com.norsedreki.dogcat.state.DogcatState
import com.norsedreki.dogcat.state.DogcatState.Active
import com.norsedreki.dogcat.state.DogcatState.Inactive
import com.norsedreki.dogcat.state.LogFiltersState
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlin.coroutines.coroutineContext
import kotlin.test.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class Dogcat(
    private val logLines: LogLines,
    private val filters: LogFiltersState,
    private val shell: Shell
) {
    private val stateSubject = MutableStateFlow<DogcatState>(Inactive)

    val state =
        stateSubject.asStateFlow().onCompletion { Logger.d("${context()} (5) COMPLETION, state") }

    private lateinit var isDeviceOnline: Flow<Boolean>

    suspend operator fun invoke(command: Command) {
        Logger.d("${context()} Dogcat got command: $command")

        when (command) {
            is Start -> {
                start(command)
            }
            ClearLogs -> {
                stateSubject.emit(Inactive)

                shell.clearLogLines()
                captureLogLines()
            }
            is FilterBy -> {
                stateSubject.emit(Inactive)

                filters.apply(command.filter)

                if (command.filter is Substring) {
                    captureLogLines(restartSource = false)
                } else {
                    captureLogLines()
                }
            }
            is ResetFilter -> {
                stateSubject.emit(Inactive)

                filters.reset(command.filterClass)

                if (command.filterClass == Substring::class) {
                    captureLogLines(restartSource = false)
                } else {
                    captureLogLines()
                }
            }
            Stop -> {
                stateSubject.emit(Inactive)

                logLines.stop()
            }
        }
    }

    private suspend fun start(subcommand: Start) {
        shell.validateShellOrThrow()
        collectDeviceOnline()

        when (subcommand) {
            is PickForegroundApp -> {
                val packageName = shell.foregroundPackageName()
                val appId = shell.appIdFor(packageName)

                filters.apply(ByPackage(packageName, appId))

                Logger.d("${context()} Start with foreground app '$packageName', app ID '$appId'")
            }
            is PickAppPackage -> {
                stateSubject.emit(Inactive)

                val packageName = subcommand.packageName
                val appId = shell.appIdFor(packageName)

                filters.apply(ByPackage(packageName, appId))

                Logger.d("Start with package name '$packageName', app ID '$appId'")
            }
            is PickAllApps -> {
                Logger.d("${context()} Start with no package filters")
            }
        }

        captureLogLines()
    }

    private suspend fun collectDeviceOnline() {
        if (this::isDeviceOnline.isInitialized) {
            fail("Calling a 'Start' command is allowed only once")
        }

        val scope = CoroutineScope(coroutineContext)
        isDeviceOnline = shell.isDeviceOnline().shareIn(scope, Lazily, 1)

        scope.launch {
            isDeviceOnline
                .runningFold(true) { acc, value ->
                    val isDeviceOnlineAgain = value && !acc

                    if (isDeviceOnlineAgain) {
                        stateSubject.emit(Inactive)
                        captureLogLines()
                    }
                    value
                }
                .collect()
        }
    }

    private suspend fun captureLogLines(restartSource: Boolean = true) {
        Logger.d("${context()} Dogcat, captureLogLines with restartSource=$restartSource")

        val filterLines = logLines.capture(restartSource)

        val device =
            Device(
                shell.deviceName(),
                isDeviceOnline,
            )

        val active =
            Active(
                filterLines.onCompletion {
                    Logger.d("${context()} COMPLETION (4): Capturing input filterLines $it")
                },
                filters.state,
                device,
            )

        stateSubject.emit(active)
    }
}
