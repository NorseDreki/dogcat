package ui.status

import AppStateFlow
import userInput.Input
import userInput.Keymap.Actions.*
import dogcat.Command
import dogcat.Dogcat
import dogcat.LogFilter
import dogcat.state.PublicState.CapturingInput
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import logger.Logger
import logger.context
import userInput.Keymap
import kotlin.coroutines.coroutineContext

class StatusPresenter(
    private val dogcat: Dogcat,
    private val appStateFlow: AppStateFlow,
    private val input: Input,
    private val ui: CoroutineDispatcher
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
            .filterIsInstance<CapturingInput>()
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
            .filterIsInstance<CapturingInput>()
            .mapLatest { it }
            .onEach {
                view.updateAutoscroll(appStateFlow.state.value.autoscroll)

                Logger.d("${context()} !Emulator ${it.deviceName}")
                view.updateDevice(it.deviceName, true)
            }
            .launchIn(scope)

        input
            .keypresses
            .filter {
                Keymap.bindings[it] == InputFilterBySubstring
            }
            .onEach {
                val filterString = view.inputFilter()

                dogcat(Command.FilterBy(LogFilter.Substring(filterString)))
            }
            .launchIn(scope)

        appStateFlow
            .state
            .onEach {
                withContext(ui) {
                    view.updateAutoscroll(it.autoscroll)

                    val p = if (it.packageFilter.second) {
                        it.packageFilter.first!!.packageName
                    } else {
                        ""
                    }
                    view.updatePackageName(p)
                }
            }
            .launchIn(scope)

        /*dogcat
            .state
            .filterIsInstance<CapturingInput>()
            .flatMapLatest { it.heartbeat }
            .onEach {
                view.updateDevice("Device", it)
            }
            .launchIn(scope)*/
    }

    suspend fun stop() {
        view.stop()
    }
}
