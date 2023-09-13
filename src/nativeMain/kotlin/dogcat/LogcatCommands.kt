package dogcat

sealed interface LogcatCommands

sealed interface Filter : LogcatCommands {
    data class ByString(val substring: String) : Filter

    data class ToggleLogLevel(val level: String) : Filter

    //last session --- //previous session
}

data class FilterWith(val filter: Filter) : LogcatCommands

data class ClearFilter(val filter: Filter) : LogcatCommands

sealed interface StartupAs : LogcatCommands {

    data object WithForegroundApp : StartupAs

    data object All : StartupAs

    data class WithPackage(val packageName: String) : StartupAs
}

data object ClearLogs : LogcatCommands

data object StopEverything : LogcatCommands