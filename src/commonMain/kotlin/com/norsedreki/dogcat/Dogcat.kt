package com.norsedreki.dogcat

import com.norsedreki.dogcat.Command.*
import com.norsedreki.dogcat.Command.Start.*
import com.norsedreki.dogcat.LogFilter.ByPackage
import com.norsedreki.dogcat.LogFilter.Substring
import com.norsedreki.dogcat.state.AppliedFiltersState
import com.norsedreki.dogcat.state.Device
import com.norsedreki.dogcat.state.PublicState.*
import com.norsedreki.dogcat.state.PublicState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context

class Dogcat(
    private val filters: AppliedFiltersState,
    private val logLines: LogLines,
    private val shell: Shell
)  {

    private val stateSubject = MutableStateFlow<PublicState>(Inactive)
    val state = stateSubject.asStateFlow().onCompletion { Logger.d("${context()} (5) COMPLETION, state") }

    suspend operator fun invoke(command: Command) {
        Logger.d("${context()} Command $command")

        when (command) {
            is Start -> {
                start(command)
            }

            ClearLogs -> {
                stateSubject.emit(Inactive)

                // keyboard input hangs upon clearing? when no emulators
                shell.clearLogLines()
                captureLogLines()
            }

            is FilterBy -> {
                stateSubject.emit(Inactive)

                // Do not re-capture log lines if filter hasn't changed
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

            Stop -> { // clear pad?
                logLines.stop()
                stateSubject.emit(Terminated)
            }
        }
    }

    private suspend fun start(subcommand: Start) {
        shell.validateShellOrThrow()

        val running = shell.deviceRunning().first()

        if (!running) {
            val ci = Terminated
            stateSubject.emit(ci)
            return
        }

        when (subcommand) {
            is PickForegroundApp -> {
                val packageName = shell.foregroundPackageName()
                val userId = shell.appIdFor(packageName)

                filters.apply(ByPackage(packageName, userId))
                Logger.d("${context()} Startup with foreground app, resolved to package '$packageName' and user ID '$userId'")
            }

            is PickAppPackage -> {
                stateSubject.emit(Inactive)

                val packageName = subcommand.packageName
                val userId = shell.appIdFor(packageName)

                filters.apply(ByPackage(packageName, userId))
                Logger.d("Startup package name '$packageName', resolved user ID to '$userId'")
            }

            is PickAllApps -> {
                Logger.d("${context()} Startup with no package filters")
            }
        }

        captureLogLines()
    }

    private suspend fun captureLogLines(restartSource: Boolean = true) {
        val filterLines = logLines.capture(restartSource)
        Logger.d("${context()} created shared lines in dogcat $filterLines")

        val device = Device(
            shell.deviceName(),
            shell.deviceRunning()
        )

        val ci = Active(
            filterLines
                .onCompletion { Logger.d("${context()} (1) COMPLETED: Capturing input filterLines $it") },

            filters.applied,

            device
        )

        stateSubject.emit(ci)
    }
}
