package ui.status

import AppStateFlow
import Environment
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
    private val environment: Environment,
    private val input: Input,
    private val scope: CoroutineScope,
    private val ui: CloseableCoroutineDispatcher
) {
    //views can come and go, when input disappears
    private val view = StatusView()

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
     fun start() {
        //what is tail-call as in launchIn?

        dogcat
            .state
            .filterIsInstance<PublicState.CapturingInput>()
            .flatMapLatest { it.applied }
            .onEach {
                view.updateFilters(it)

                it[LogFilter.ByPackage::class]?.let {
                    appStateFlow.filterByPackage(it as LogFilter.ByPackage, true)
                }
            }
            .launchIn(scope)


        dogcat
            .state
            .filterIsInstance<PublicState.CapturingInput>()
            .mapLatest { it }
            .onEach {
                view.updateAutoscroll(appStateFlow.state.value.autoscroll)

                Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] !Emulator ${it.deviceName}")
                view.updateDevice(it.deviceName, true)
            }
            .launchIn(scope)

        input
            .keypresses
            .filter { it == 'f'.code } //add escape to cancel filter
            .onEach {
                val filterString = withContext(ui) { view.inputFilter()}

                dogcat(Command.FilterBy(LogFilter.Substring(filterString)))
            }
            .launchIn(scope)

        appStateFlow
            .state
            .onEach {
                withContext(ui) {
                    view.updateAutoscroll(it.autoscroll)
                }
            }
            .launchIn(scope)

        environment
            .heartbeat()
            .onEach {
                view.updateDevice("Device", it)
            }
            .launchIn(scope)
    }

    suspend fun stop() {
        view.stop()
    }
}
