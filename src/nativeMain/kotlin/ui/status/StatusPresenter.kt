package ui.status

import AppStateFlow
import Input
import dogcat.Command
import dogcat.Dogcat
import dogcat.LogFilter
import dogcat.PublicState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class StatusPresenter(
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
            .filterIsInstance<PublicState.CapturingInput>()
            .flatMapLatest { it.applied }
            .onEach { view.updateFilters(it) }
            .launchIn(scope)

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
