import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppState(
    val autoscroll: Boolean,
    //isPackageFilteringEnabled
    //val linesCount: Int,
)

interface AppStateFlow {

    val state: StateFlow<AppState>

    //isPackageFilteringEnabled

    fun autoscroll(on: Boolean)
}

class InternalAppStateFlow : AppStateFlow {
    override val state = MutableStateFlow(AppState(false))


    override fun autoscroll(on: Boolean) {
        state.value = state.value.copy(autoscroll = on)
    }
}
