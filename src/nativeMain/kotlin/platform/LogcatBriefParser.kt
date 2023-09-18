package platform

import Config
import Config.tagWidth
import dogcat.LogLine
import dogcat.LogLineParser
import dogcat.Original
import dogcat.Parsed

class LogcatBriefParser : LogLineParser {
    val r2 = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

    override fun parse(line: String): LogLine {
        val m = r2.matchEntire(line)

        return if (m != null) {
            val (level, tag, owner, message) = m.destructured //use newest feature, 'named capturing groups'
            Parsed(level, tag.massage(), owner, message)
        } else {
            Original(line)
        }
    }

    // or maybe create a dedicated Tag class
    private fun String.massage(): String {
        return if (length > tagWidth) {
            val excess = 1 - tagWidth % 2
            // performance impact?
            take(tagWidth / 2 - excess) + Typography.ellipsis + takeLast(tagWidth / 2)
        } else {
            trim().padStart(tagWidth)
        }
    }
}
