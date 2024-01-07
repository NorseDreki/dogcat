package dogcat

sealed interface LogLine
data class BriefLogLine(
    val level: String,
    val tag: String,
    val owner: String,
    val message: String
) : LogLine

data class UnparseableLogLine(val line: String) : LogLine



interface LogLineParser {
    fun parse(line: String) : LogLine
}

enum class LogLevel {
    V, D, I, W, E, F
}
