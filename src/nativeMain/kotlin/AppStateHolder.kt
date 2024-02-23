import dogcat.LogFilter.ByPackage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppStateHolder(

    val autoscroll: Boolean,

    val packageFilter: Pair<ByPackage?, Boolean>,

    val inputFilterLocation: Pair<Int, Int>,

    val isCursorHeld: Boolean
)

interface AppState {

    val state: StateFlow<AppStateHolder>

    fun autoscroll(on: Boolean)

    fun filterByPackage(f: ByPackage?, enable: Boolean)

    fun setInputFilterLocation(x: Int, y: Int)

    fun holdCursor(hold: Boolean)
}

class InternalAppState : AppState {

    override val state = MutableStateFlow(
        AppStateHolder(
            false,
            null to false,
            0 to 0,
            false
        )
    )

    override fun autoscroll(on: Boolean) {
        state.value = state.value.copy(autoscroll = on)
    }

    override fun filterByPackage(f: ByPackage?, enable: Boolean) {
        //if (f != null) {
        //    state.value = state.value.copy(packageFilter = f to true)
        //} else {
        //state.value = state.value.copy(packageFilter = state.value.packageFilter.first to enable)
        state.value = state.value.copy(packageFilter = f to enable)
        //}
    }

    override fun setInputFilterLocation(x: Int, y: Int) {
        state.value = state.value.copy(inputFilterLocation = x to y)
    }

    override fun holdCursor(hold: Boolean) {
        state.value = state.value.copy(isCursorHeld = hold)
    }
}
