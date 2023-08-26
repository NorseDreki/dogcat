sealed interface LogLine

data class Parsed(
    val level: String,
    val tag: String,
    val owner: String,
    val message: String
) : LogLine

data class Original(val line: String) : LogLine
