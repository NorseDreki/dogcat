package ui.status

import AppState
import userInput.Input
import dogcat.Command.FilterBy
import dogcat.Dogcat
import dogcat.LogFilter
import dogcat.LogFilter.ByPackage
import dogcat.LogFilter.Substring
import dogcat.state.PublicState.Active
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import logger.Logger
import logger.context
import kotlin.coroutines.coroutineContext

class StatusPresenter(
    private val dogcat: Dogcat,
    private val appState: AppState,
    private val input: Input
) {
    //views can come and go, when input disappears
    private lateinit var view: StatusView

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun start() {
        view = StatusView()
        view.start()

        val scope = CoroutineScope(coroutineContext)

        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.applied }
            .onEach {
                //view.updateFilters(it)
                view.state = view.state.copy(filters = it)

                Logger.d("${context()} Update filters in pres")
                it[ByPackage::class]?.let {
                    appState.filterByPackage(it as ByPackage, true)
                }
            }
            .launchIn(scope)


        dogcat
            .state
            .filterIsInstance<Active>()
            .mapLatest { it }
            .onEach {
                view.state = view.state.copy(autoscroll = appState.state.value.autoscroll)

                //view.updateAutoscroll(appState.state.value.autoscroll)

                Logger.d("${context()} !Emulator in pres ${it.deviceName}")
                //view.updateDevice(it.deviceName, true)

                view.state = view.state.copy(emulator = it.deviceName, running = true)
            }
            .launchIn(scope)


        //or just allow for filtering since lines are already cached in sharedLines?
        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.heartbeat }
            .filter { it }
            .distinctUntilChanged()
            .flatMapLatest {
                input.strings
            }
            .onEach {
                dogcat(FilterBy(Substring(it)))
            }
            .launchIn(scope)


        appState
            .state
            .onEach {
                Logger.d("${context()} autoscroll in pres ${it.autoscroll}")
                //view.state = view.state.copy(autoscroll = it.autoscroll)
                //view.updateAutoscroll(it.autoscroll)

                val p = if (it.packageFilter.second) {
                    it.packageFilter.first!!.packageName
                } else {
                    ""
                }
                //view.updatePackageName(p)

                view.state = view.state.copy(
                    packageName = p,
                    autoscroll = it.autoscroll,
                    isCursorHeld = it.isCursorHeld
                )

            }
            .launchIn(scope)


        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.heartbeat }
            .onEach {
                //view.updateDevice("Device", it)

                //view.state = view.state.copy(emulator = "DEVICE", running = it)
            }
            .launchIn(scope)
    }

    suspend fun stop() {
        view.stop()
    }
}
