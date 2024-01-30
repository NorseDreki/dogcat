package dogcat

import kotlin.reflect.KClass

sealed interface Command {
    sealed interface Start : Command {
        data object PickAllApps : Start
        data object PickForegroundApp : Start
        data class PickAppPackage(val packageName: String) : Start
    }

    data class FilterBy(val filter: LogFilter) : Command
    data class ResetFilter(val filterClass: KClass<out LogFilter>) : Command
    data object ClearLogSource : Command

    data object Stop : Command
}
