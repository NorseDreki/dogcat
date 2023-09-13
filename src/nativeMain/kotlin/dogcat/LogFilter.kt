package dogcat

sealed interface LogFilter {

    fun apply(line: Parsed): Boolean

    data class BySubstring(val substring: String) : LogFilter {
        override fun apply(line: Parsed) = line.message.contains(substring)
    }

    data class ByLogLevels(val substring: String) : LogFilter {
        override fun apply(line: Parsed): Boolean {
            TODO("Not yet implemented")
        }
    }

    data class ByExcludedTags(val exclusions: List<String>) : LogFilter {
        override fun apply(line: Parsed): Boolean {
            TODO("Not yet implemented")
        }

    }

    data class LastSession(val exclusions: List<String>) : LogFilter {
        override fun apply(line: Parsed): Boolean {
            TODO("Not yet implemented")
        }
    }

    data class ByTime(val l: String) : LogFilter {
        override fun apply(line: Parsed): Boolean {
            TODO("Not yet implemented")
        }

    }

    //Do not include PIDs since no restart needed
}
