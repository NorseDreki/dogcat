package dogcat

import kotlin.reflect.KClass

sealed interface Command {
    data class FilterBy(val filter: LogFilter) : Command
    data class ResetFilter(val filter: KClass<out LogFilter>) : Command

    sealed interface StartupAs : Command {
        data object WithForegroundApp : StartupAs
        data object All : StartupAs
        data class WithPackage(val packageName: String) : StartupAs
    }

    data object ClearLogs : Command
    data object StopEverything : Command
}
