package dogcat

sealed interface LogFilter {
    data class Substring(val substring: String) : LogFilter {
        override fun equals(other: Any?): Boolean {
            println("!! $other equlas?")
            if (other == null) return false
            if (this === other) return true
            return (this::class == other::class)
        }
        override fun hashCode() = this::class.hashCode()
    }

    data class MinLogLevel(val logLevel: String) : LogFilter

    data class ByPackage(val packageName: String, val resolvedUserId: String) : LogFilter
}
