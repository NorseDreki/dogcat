package dogcat

import kotlinx.coroutines.flow.Flow

data class LogLine(
    val level: String,
    val tag: String,
    val owner: String,
    val message: String
)

interface LogLineParser {
    fun parse(line: String) : LogLine
}

interface LogLinesSource {
    fun lines(): Flow<String>
}

enum class LogLevel {
    V, D, I, W, E, F
}

