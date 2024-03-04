package ui.status

import AppState
import com.norsedreki.dogcat.Command.FilterBy
import com.norsedreki.dogcat.Dogcat
import com.norsedreki.dogcat.LogFilter.ByPackage
import com.norsedreki.dogcat.LogFilter.Substring
import com.norsedreki.dogcat.state.DogcatState.Active
import com.norsedreki.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ui.HasLifecycle
import userInput.Input
import kotlin.coroutines.coroutineContext

class StatusPresenter(
    private val dogcat: Dogcat,
    private val appState: AppState,
    private val input: Input
) : HasLifecycle {

    //views can come and go, when input disappears
    private lateinit var view: StatusView

    override suspend fun start() {
        view = StatusView()
        view.start()

        val scope = CoroutineScope(coroutineContext)

        scope.launch {
            collectDogcatState()
        }
        scope.launch {
            collectUserStringInput()
        }
        scope.launch {
            collectAppState()
        }
        scope.launch {
            collectDeviceStatus()
        }
    }

    override suspend fun stop() {
        //also cancel scope?
        if (this::view.isInitialized) {
            view.stop()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectDogcatState() {
        dogcat
            .state
            .filterIsInstance<Active>()
            .mapLatest { it }
            .onEach {
                val filters = it.filters.first()

                filters[ByPackage::class]?.let {
                    appState.filterByPackage(it as ByPackage, true)
                }

                Logger.d("Pres active device label: ${it.device.label}")
                Logger.d(">>>>>>>>>>>----- UPDATE STATUS VIEW STATE,  DogcatState $it")

                view.state = view.state.copy(
                    filters = filters,
                    autoscroll = appState.state.value.autoscroll,
                    deviceLabel = it.device.label,
                    isDeviceOnline = it.device.isOnline.first()
                )
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectUserStringInput() {
        //or just allow for filtering since lines are already cached in sharedLines?
        // it's already allowed
        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.device.isOnline }
            .filter { it }
            .distinctUntilChanged()
            .flatMapLatest { input.strings }
            .onEach {
                dogcat(FilterBy(Substring(it)))
            }
            .collect()
    }

    private suspend fun collectAppState() {
        appState
            .state
            .onEach {
                val packageName = if (it.packageFilter.second) {
                    it.packageFilter.first!!.packageName
                } else {
                    ""
                }

                Logger.d(">>>>>>>>>>>----- UPDATE STATUS VIEW STATE,  App state $it")
                view.state = view.state.copy(
                    packageName = packageName,
                    autoscroll = it.autoscroll,
                    isCursorHeld = it.isCursorHeld,
                    cursorReturnLocation = it.inputFilterLocation
                )

            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectDeviceStatus() {
        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.device.isOnline }
            .distinctUntilChanged()
            .onEach {
                Logger.d(">>>>>>>>>>>----- UPDATE STATUS VIEW STATE,  Device status $it")
                view.state = view.state.copy(isDeviceOnline = it)
            }
            .collect()
    }
}
