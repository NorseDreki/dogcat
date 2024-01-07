package dogcat

import Logger

class LogcatBriefParser : LogLineParser {
    val r2 = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

    override fun parse(line: String): LogLine {
        val m = r2.matchEntire(line)

        return if (m != null) {
            val (level, tag, owner, message) = m.destructured //use newest feature, 'named capturing groups'
            BriefLogLine(level, tag, owner, message)
        } else {
            Logger.d("Unparseable log line: '$line'")
            UnparseableLogLine(line)
        }
    }
}
