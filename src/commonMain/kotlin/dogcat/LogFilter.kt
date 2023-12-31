package dogcat

sealed interface LogFilter {
    data class Substring(val substring: String) : LogFilter
    data class MinLogLevel(val logLevel: String) : LogFilter
    data class ByPackage(val packageName: String, val resolvedUserId: String) : LogFilter
}
