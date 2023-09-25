package dogcat

import dogcat.Command.*
import dogcat.Command.Start.*
import dogcat.LogFilter.ByPackage
import dogcat.LogFilter.Substring
import dogcat.PublicState.*
import flow.takeUntil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import platform.DumpsysPackage
import platform.ForegroundProcess
import platform.Logger

class Dogcat(
    private val filters: InternalAppliedFiltersState = InternalAppliedFiltersState(),
    private val logLines: LogLines,
) : State by InternalState() {

    private val stateSubject = MutableStateFlow<PublicState>(WaitingInput)
    val state = stateSubject.asStateFlow()

    private val stopSubject = MutableSharedFlow<Unit>()

    suspend operator fun invoke(command: Command) {
        when (command) {

            is Start -> start(command)

            ClearLogSource -> {
                Logger.d("to clear logs\r")
                logLines.logLinesSource.clear()
                stopSubject.emit(Unit)
                stateSubject.emit(InputCleared)

                captureLogLines()
            }

            is FilterBy -> {
                filters.add(command.filter)
                stateSubject.emit(InputCleared)
                captureLogLines()
            }

            is ResetFilter -> {
                when (command.filterClass) {
                    Substring::class -> filters.add(Substring("")) //use default filters
                    else -> filters.removeFilter(command.filterClass)
                }
            }

            Stop -> {
                stateSubject.emit(Stopped)
                stopSubject.emit(Unit)

                //scope.cancel()
            }
        }
    }

    private suspend fun start(command: Start) {
        when (command) {
            is SelectForegroundApp -> {
                val packageName = ForegroundProcess.parsePackageName()
                val userId = DumpsysPackage().parseUserIdFor(packageName)

                filters.add(ByPackage(packageName, userId), true)
                Logger.d("Startup with foreground app, resolved to package '$packageName' and user ID '$userId'")
            }

            is SelectAppByPackage -> {
                val packageName = command.packageName
                val userId = DumpsysPackage().parseUserIdFor(packageName)

                filters.add(ByPackage(packageName, userId), true)
                Logger.d("Startup package name '$packageName', resolved user ID to '$userId'")
            }

            is All -> {
                Logger.d("Startup with no package filters")
            }
        }

        captureLogLines()
    }

    private suspend fun captureLogLines() {
        val filterLines = logLines.filterLines()

        val ci = CapturingInput(
            filterLines
                .onCompletion { Logger.d("COMPLETED: Capturing input log lines $it\r") } //called when scope is cancelled as well
                .takeUntil(stopSubject),

            filters.applied
        )
        stateSubject.emit(ci)
    }
}
