import dogcat.LogFilter.ByPackage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppState(

    val autoscroll: Boolean,

    val packageFilter: Pair<ByPackage?, Boolean>

    //val linesCount: Int,
)

interface AppStateFlow {

    val state: StateFlow<AppState>

    fun autoscroll(on: Boolean)

    fun filterByPackage(f: ByPackage?, enable: Boolean)
}

class InternalAppStateFlow : AppStateFlow {

    override val state = MutableStateFlow(AppState(false, null to false))

    override fun autoscroll(on: Boolean) {
        state.value = state.value.copy(autoscroll = on)
    }

    override fun filterByPackage(f: ByPackage?, enable: Boolean) {
        if (f != null) {
            state.value = state.value.copy(packageFilter = f to true)
        } else {
            state.value = state.value.copy(packageFilter = state.value.packageFilter.first to enable)
        }
    }
}
