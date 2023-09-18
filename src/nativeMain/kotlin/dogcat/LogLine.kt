package dogcat

data class LogLine(
    val level: String,
    val tag: String,
    val owner: String,
    val message: String
)
