package com.norsedreki.dogcat

import com.norsedreki.dogcat.Command.*
import com.norsedreki.dogcat.Command.Start.*
import com.norsedreki.dogcat.LogFilter.ByPackage
import com.norsedreki.dogcat.LogFilter.Substring
import com.norsedreki.dogcat.state.Device
import com.norsedreki.dogcat.state.DogcatState
import com.norsedreki.dogcat.state.DogcatState.Active
import com.norsedreki.dogcat.state.DogcatState.Inactive
import com.norsedreki.dogcat.state.LogFiltersState
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion

class Dogcat(
    private val logLines: LogLines,
    private val filters: LogFiltersState,
    private val shell: Shell
) {
    private val stateSubject = MutableStateFlow<DogcatState>(Inactive)

    val state = stateSubject.asStateFlow()
        .onCompletion {
            Logger.d("${context()} (5) COMPLETION, state")
        }

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

        when (subcommand) {
            is PickForegroundApp -> {
                val packageName = shell.foregroundPackageName()
                val appId = shell.appIdFor(packageName)

                filters.apply(ByPackage(packageName, appId))

                Logger.d("${context()} Start with foreground app '$packageName', app ID '$appId'")
            }

            is PickAppPackage -> {
                //stateSubject.emit(Inactive)

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

    private suspend fun captureLogLines(restartSource: Boolean = true) {
        Logger.d("${context()} Dogcat, captureLogLines with restartSource=$restartSource")

        val filterLines = logLines.capture(restartSource)

        val label = shell.deviceName()
        Logger.d("cll device name $label")

        val device = Device(
            label,
            shell.deviceRunning()
        )

        val active = Active(
            filterLines
                .onCompletion { Logger.d("${context()} (4) COMPLETED: Capturing input filterLines $it") },

            filters.state,

            device
        )

        stateSubject.emit(active)
    }
}
