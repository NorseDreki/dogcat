interface LogLineParser {
    fun parse(line: String) : LogLine
}

class LogcatBriefParser : LogLineParser {
    val r2 = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

    override fun parse(line: String): LogLine {
        val m = r2.matchEntire(line)

        return if (m != null) {
            val (level, tag, owner, message) = m.destructured //use newest feature, 'named capturing groups'
            Parsed(level, tag.trim().padStart(40), owner, message)
        } else {
            Original(line)
        }
    }
}
