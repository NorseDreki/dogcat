package dogcat

import dogcat.Command.*
import dogcat.Command.Start.*
import dogcat.LogFilter.ByPackage
import dogcat.LogFilter.Substring
import dogcat.state.PublicState.*
import dogcat.state.DefaultAppliedFiltersState
import dogcat.state.PublicState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import logger.Logger
import logger.context

class Dogcat(
    private val filters: DefaultAppliedFiltersState = DefaultAppliedFiltersState(),
    private val logLines: LogLines,
    private val shell: Shell
)  {

    private val stateSubject = MutableStateFlow<PublicState>(WaitingInput)
    val state = stateSubject.asStateFlow().onCompletion { Logger.d("${context()} (5) COMPLETION, state") }

    suspend operator fun invoke(command: Command) {
        Logger.d("${context()} Command $command")

        when (command) {
            is Start -> start(command)

            ClearLogSource -> {
                // keyboard input hangs upon clearing? when no emulators
                shell.clearSource()
                stateSubject.emit(InputCleared)

                captureLogLines()
            }

            is FilterBy -> {
                stateSubject.emit(InputCleared)

                // Do not re-capture log lines if filter hasn't changed
                filters.apply(command.filter)


                if (command.filter is Substring) {
                    captureLogLines(restartSource = false)
                } else {
                    captureLogLines()
                }
            }

            is ResetFilter -> {
                filters.reset(command.filterClass)

                stateSubject.emit(InputCleared)

                if (command.filterClass == Substring::class) {
                    captureLogLines(restartSource = false)
                } else {
                    captureLogLines()
                }
            }

            Stop -> { // clear pad?
                stateSubject.emit(Stopped)
            }
        }
    }

    private suspend fun start(subcommand: Start) {
        val shellAvailable = shell.isShellAvailable()

        val running = shell.heartbeat().first()

        if (!shellAvailable || !running) {
            val ci = Stopped
            stateSubject.emit(ci)
            return
        }

        when (subcommand) {
            is PickForegroundApp -> {
                val packageName = shell.foregroundPackageName()
                val userId = shell.userIdFor(packageName)

                filters.apply(ByPackage(packageName, userId))
                Logger.d("Startup with foreground app, resolved to package '$packageName' and user ID '$userId'")
            }

            is PickApp -> {
                stateSubject.emit(InputCleared)

                val packageName = subcommand.packageName
                val userId = shell.userIdFor(packageName)

                filters.apply(ByPackage(packageName, userId))
                Logger.d("Startup package name '$packageName', resolved user ID to '$userId'")
            }

            is All -> {
                Logger.d("Startup with no package filters")
            }
        }

        captureLogLines()
    }

    private suspend fun captureLogLines(restartSource: Boolean = true) {
        val filterLines = logLines.capture(restartSource)

        val deviceName = shell.currentEmulatorName()

        val ci = CapturingInput(
            filterLines
                .onCompletion { Logger.d("${context()} (1) COMPLETED: Capturing input filterLines $it") },

            filters.applied,

            deviceName,

            shell.heartbeat()
        )

        stateSubject.emit(ci)
    }
}
