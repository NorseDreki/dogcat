sealed interface LogcatCommands {

}

sealed interface Filter {

    data class Package(val name: String) : Filter

    data class ByString(val substring: String) : Filter

    data class ByLogLevel(val logLevels: Set<String>) : Filter

    data class ByTime(val logLevels: Set<String>) : Filter

    //last session

    //previous session
}

data class FilterWith(val filter: Filter) : LogcatCommands

data class ClearFilter(val filter: Filter) : LogcatCommands

data class StartupAs(val filter: Filter) : LogcatCommands {

    data object WithForegroundApp //: StartupAs()

    data object All

    data object WithPackage
}

data object ClearLogs : LogcatCommands

data class Exclude(val e: String) : LogcatCommands // or treat as filter
