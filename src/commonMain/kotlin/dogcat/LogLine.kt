package dogcat

sealed interface LogLine

data class Brief(
    val level: LogLevel,
    val tag: String,
    val owner: String,
    val message: String
) : LogLine

data class Unparseable(val line: String) : LogLine

enum class LogLevel {
    V, D, I, W, E, F
}
