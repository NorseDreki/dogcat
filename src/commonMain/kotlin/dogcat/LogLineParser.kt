package dogcat

import logger.Logger

interface LogLineParser {
    fun parse(line: String) : LogLine
}


class LogcatBriefParser : LogLineParser {
    private val briefPattern = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

    override fun parse(line: String): LogLine {
        val m = briefPattern.matchEntire(line)

        return if (m != null) {
            // As an option, use 'named capturing groups' feature of 1.9, but it looks more verbose
            val (level, tag, owner, message) = m.destructured

            Brief(
                LogLevel.valueOf(level),
                tag,
                owner,
                message
            )
        } else {
            Logger.d("[LogLineParser] Unparseable log line: '$line'")

            Unparseable(line)
        }
    }
}
