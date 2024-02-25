package ui.status

import AppState
import dogcat.Command.FilterBy
import dogcat.Dogcat
import dogcat.LogFilter.ByPackage
import dogcat.LogFilter.Substring
import dogcat.state.PublicState.Active
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import logger.Logger
import logger.context
import userInput.Input
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

        //scope.launch?
        dogcat
            .state
            .filterIsInstance<Active>()
            //.take(1)
            .mapLatest { it }
            .onEach {
                val filters = it.applied.first()

                filters[ByPackage::class]?.let {
                    appState.filterByPackage(it as ByPackage, true)
                }

                view.state = view.state.copy(
                    filters = filters,
                    autoscroll = appState.state.value.autoscroll,
                    emulator = it.deviceName,
                    running = true
                )
            }
            .launchIn(scope)


        //or just allow for filtering since lines are already cached in sharedLines?
        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.heartbeat }
            .filter { it }
            .distinctUntilChanged()
            .flatMapLatest { input.strings }
            .onEach {
                dogcat(FilterBy(Substring(it)))
            }
            .launchIn(scope)


        appState
            .state
            .onEach {
                Logger.d("${context()} autoscroll in pres ${it.autoscroll}")

                val p = if (it.packageFilter.second) {
                    it.packageFilter.first!!.packageName
                } else {
                    ""
                }

                view.state = view.state.copy(
                    packageName = p,
                    autoscroll = it.autoscroll,
                    isCursorHeld = it.isCursorHeld,
                    cursorReturnLocation = it.inputFilterLocation
                )

            }
            .launchIn(scope)


        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.heartbeat }
            .distinctUntilChanged()
            .onEach {
                view.state = view.state.copy(emulator = "DEVICE", running = it)
            }
            .launchIn(scope)
    }

    fun stop() {
        view.stop()
    }
}
