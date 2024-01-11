package dogcat

import kotlin.reflect.KClass

sealed interface Command {
    sealed interface Start : Command {
        data object PickForegroundApp : Start
        data object All : Start
        data class PickApp(val packageName: String) : Start
    }
    data object Stop : Command

    data class FilterBy(val filter: LogFilter) : Command
    data class ResetFilter(val filterClass: KClass<out LogFilter>) : Command
    data object ClearLogSource : Command
}
