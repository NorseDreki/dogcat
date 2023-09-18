package dogcat

sealed interface Command {
    data class FilterBy(val filter: LogFilter) : Command
    data class ClearFilter(val filter: LogFilter) : Command

    sealed interface StartupAs : Command {
        data object WithForegroundApp : StartupAs
        data object All : StartupAs
        data class WithPackage(val packageName: String) : StartupAs
    }

    data object ClearLogs : Command
    data object StopEverything : Command
}
