package dogcat

import Environment
import dogcat.Command.*
import dogcat.Command.Start.*
import dogcat.LogFilter.ByPackage
import dogcat.LogFilter.Substring
import dogcat.PublicState.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import Logger

@OptIn(ExperimentalStdlibApi::class)
class Dogcat(
    private val filters: InternalAppliedFiltersState = InternalAppliedFiltersState(),
    private val logLines: LogLines,
    private val environment: Environment
)  {

    private val stateSubject = MutableStateFlow<PublicState>(WaitingInput)
    val state = stateSubject.asStateFlow().onCompletion { Logger.d("[${ctx()}] (5) COMPLETION, state") }

    suspend operator fun invoke(command: Command) {
        Logger.d("[${ctx()}] Command $command")

        when (command) {
            is Start -> start(command)

            ClearLogSource -> {
                // keyboard input hangs upon clearing? when no emulators
                environment.clearSource()
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
        when (subcommand) {
            is PickForegroundApp -> {
                val packageName = environment.foregroundPackageName()
                val userId = environment.userIdFor(packageName)

                filters.apply(ByPackage(packageName, userId))
                Logger.d("Startup with foreground app, resolved to package '$packageName' and user ID '$userId'")
            }

            is PickApp -> {
                stateSubject.emit(InputCleared)

                val packageName = subcommand.packageName
                val userId = environment.userIdFor(packageName)

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

        val deviceName = environment.currentEmulatorName()

        val ci = CapturingInput(
            filterLines
                .onCompletion { Logger.d("[${ctx()}] (1) COMPLETED: Capturing input filterLines $it\r") },

            filters.applied,

            deviceName
        )
        stateSubject.emit(ci)
    }

    private suspend fun ctx() = currentCoroutineContext()[CoroutineDispatcher]
}
