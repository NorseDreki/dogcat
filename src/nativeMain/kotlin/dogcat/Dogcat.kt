package dogcat

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
import platform.DumpsysPackage
import platform.EmulatorName
import platform.ForegroundProcess
import platform.Logger

@OptIn(ExperimentalStdlibApi::class)
class Dogcat(
    private val filters: InternalAppliedFiltersState = InternalAppliedFiltersState(),
    private val logLines: LogLines,
)  {

    private val stateSubject = MutableStateFlow<PublicState>(WaitingInput)
    val state = stateSubject.asStateFlow().onCompletion { Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] (5) COMPLETION, state") }

    suspend operator fun invoke(command: Command) {
        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Command $command")

        when (command) {
            is Start -> start(command)

            ClearLogSource -> {
                logLines.logLinesSource.clear()
                stateSubject.emit(InputCleared)

                captureLogLines()
            }

            is FilterBy -> {
                stateSubject.emit(InputCleared)

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
            is SelectForegroundApp -> {
                val packageName = ForegroundProcess.parsePackageName()
                val userId = DumpsysPackage().parseUserIdFor(packageName)

                filters.apply(ByPackage(packageName, userId))
                Logger.d("Startup with foreground app, resolved to package '$packageName' and user ID '$userId'")
            }

            is SelectAppByPackage -> {
                stateSubject.emit(InputCleared)

                val packageName = subcommand.packageName
                val userId = DumpsysPackage().parseUserIdFor(packageName)

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

        val deviceName = EmulatorName.currentEmulatorName()

        val ci = CapturingInput(
            filterLines
                .onCompletion { Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] (1) COMPLETED: Capturing input filterLines $it\r") },

            filters.applied,

            deviceName
        )
        stateSubject.emit(ci)
    }
}
