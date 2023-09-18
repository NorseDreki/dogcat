package platform

import dogcat.LogLine
import dogcat.LogLineParser

class LogcatBriefParser : LogLineParser {
    val r2 = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

    override fun parse(line: String): LogLine {
        val m = r2.matchEntire(line)

        return if (m != null) {
            val (level, tag, owner, message) = m.destructured //use newest feature, 'named capturing groups'
            LogLine(level, tag, owner, message)
        } else {
            Logger.d("! Unparseable log line: '$line'")
            LogLine("?", "<>", "", line)
        }
    }
}
