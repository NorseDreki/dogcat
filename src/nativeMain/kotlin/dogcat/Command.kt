package dogcat

import LogFilter
import kotlin.reflect.KClass

sealed interface Command {
    sealed interface Start : Command {
        data object SelectForegroundApp : Start
        data object All : Start
        data class SelectAppByPackage(val packageName: String) : Start
    }
    data object Stop : Command

    data class FilterBy(val filter: LogFilter) : Command
    data class ResetFilter(val filterClass: KClass<out LogFilter>) : Command
    data object ClearLogSource : Command
}
