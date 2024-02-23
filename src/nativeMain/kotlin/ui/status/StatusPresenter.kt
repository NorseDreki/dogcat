package ui.status

import AppStateFlow
import userInput.Input
import userInput.Keymap.Actions.*
import dogcat.Command
import dogcat.Command.FilterBy
import dogcat.Dogcat
import dogcat.LogFilter
import dogcat.LogFilter.Substring
import dogcat.state.PublicState.Active
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import logger.Logger
import logger.context
import userInput.Keymap
import kotlin.coroutines.coroutineContext

class StatusPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
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
                view.updateFilters(it)

                Logger.d("${context()} Update filters in pres")
                it[LogFilter.ByPackage::class]?.let {
                    appStateFlow.filterByPackage(it as LogFilter.ByPackage, true)
                }
            }
            .launchIn(scope)


        dogcat
            .state
            .filterIsInstance<Active>()
            .mapLatest { it }
            .onEach {
                view.updateAutoscroll(appStateFlow.state.value.autoscroll)

                Logger.d("${context()} !Emulator in pres ${it.deviceName}")
                view.updateDevice(it.deviceName, true)
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
                view.inputFilter1()

                dogcat(FilterBy(Substring(it)))
            }
            .launchIn(scope)




        appStateFlow
            .state
            .onEach {
                Logger.d("${context()} autoscroll in pres ${it.autoscroll}")
                    view.updateAutoscroll(it.autoscroll)

                    val p = if (it.packageFilter.second) {
                        it.packageFilter.first!!.packageName
                    } else {
                        ""
                    }
                    view.updatePackageName(p)
            }
            .launchIn(scope)


        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.heartbeat }
            .onEach {
                //view.updateDevice("Device", it)
            }
            .launchIn(scope)
    }

    suspend fun stop() {
        view.stop()
    }
}
