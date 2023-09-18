package dogcat

interface LogLineParser {
    fun parse(line: String) : LogLine
}
