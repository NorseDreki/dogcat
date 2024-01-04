package ui.logLines

import ui.status.StatusView

import AppStateFlow
import Input
import dogcat.Command
import dogcat.Dogcat
import dogcat.LogFilter
import dogcat.PublicState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LogLinesPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val scope: CoroutineScope
) {
    //views can come and go, when input disappears
    private val view = StatusView()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun start() {
        //what is tail-call as in launchIn?

        dogcat
            .state
            .flatMapLatest {
                when (it) {
                    is PublicState.WaitingInput -> {
                        Logger.d("Waiting for log lines...\r")

                        emptyFlow()
                    }
                    is PublicState.CapturingInput -> {
                        it.lines//.take(10)
                    }
                    PublicState.InputCleared -> {
                        Logger.d("Cleared Logcat and re-started\r")
                        pad.clear()

                        emptyFlow()
                    }
                    PublicState.Stopped -> {
                        //cancel()
                        Logger.d("No more reading lines, terminated\r")
                        emptyFlow()
                    }
                }
            }
            .onEach { pad.processLogLine(it) }
            .launchIn(this)

        input
            .keypresses
            .filter { it == 'f'.code }
            .onEach {
                val filterString = view.inputFilter()

                dogcat(Command.FilterBy(LogFilter.Substring(filterString)))
            }
            .launchIn(scope)

        appStateFlow
            .state
            .onEach {

            }
            .launchIn(scope)
    }

    suspend fun stop() {
        view.stop()
    }
}
