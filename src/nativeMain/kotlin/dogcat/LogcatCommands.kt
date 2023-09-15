package dogcat

sealed interface LogcatCommands

data class FilterBy(val filter: LogFilter) : LogcatCommands

data class ClearFilter(val filter: LogFilter) : LogcatCommands

sealed interface StartupAs : LogcatCommands {

    data object WithForegroundApp : StartupAs

    data object All : StartupAs

    data class WithPackage(val packageName: String) : StartupAs
}

data object ClearLogs : LogcatCommands

data object StopEverything : LogcatCommands
