package ui.status

import AppState
import com.norsedreki.dogcat.Command.FilterBy
import com.norsedreki.dogcat.Dogcat
import com.norsedreki.dogcat.LogFilter.ByPackage
import com.norsedreki.dogcat.LogFilter.Substring
import com.norsedreki.dogcat.state.DogcatState.Active
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun start() {
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
                val filters = it.filters.first()

                filters[ByPackage::class]?.let {
                    appState.filterByPackage(it as ByPackage, true)
                }

                view.state = view.state.copy(
                    filters = filters,
                    autoscroll = appState.state.value.autoscroll,
                    deviceLabel = it.device.label,
                    running = true
                )
            }
            .launchIn(scope)


        //or just allow for filtering since lines are already cached in sharedLines?
        dogcat
            .state
            .filterIsInstance<Active>()
            .flatMapLatest { it.device.isOnline }
            .catch {  } //!!
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
            .flatMapLatest { it.device.isOnline }
            .distinctUntilChanged()
            .onEach {
                view.state = view.state.copy(running = it)
            }
            .launchIn(scope)
    }

    override suspend fun stop() {
        view.stop()
    }
}
