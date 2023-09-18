import dogcat.LogLine
import dogcat.Original
import dogcat.Parsed
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*
import kotlin.math.min

class LogLineColorizer {

    @OptIn(ExperimentalForeignApi::class)
    fun printTag(pad: Pad, tag: String) {
        val c = allocateColor(tag)

        wattron(pad.fp, COLOR_PAIR(ccpp[c]!!))
        waddstr(pad.fp, tag)
        wattroff(pad.fp, COLOR_PAIR(ccpp[c]!!))
    }

    fun allocateColor(tag: String): Int {
        if (!KNOWN_TAGS.containsKey(tag)) {
            KNOWN_TAGS[tag] = LAST_USED[0]
        }
        val color = KNOWN_TAGS[tag]!!

        if (LAST_USED.contains(color)) {
            LAST_USED.remove(color)
            LAST_USED.add(color)
        }

        return color
    }

    val LAST_USED = mutableListOf( /*COLOR_RED,*/ COLOR_GREEN, /*COLOR_YELLOW,*/ COLOR_BLUE, COLOR_MAGENTA, COLOR_CYAN)

    val ccpp = LAST_USED.map {
        init_pair((100 + it).toShort(), it.toShort(), -1)
        it to (100 +it)
    }.toMap()


val KNOWN_TAGS = mutableMapOf(
  "dalvikvm" to COLOR_WHITE,
  "Process" to COLOR_WHITE,
  "ActivityManager" to COLOR_WHITE,
  "ActivityThread" to COLOR_WHITE,
  "AndroidRuntime" to COLOR_CYAN,
  "jdwp" to COLOR_WHITE,
  "StrictMode" to COLOR_WHITE,
  "DEBUG" to COLOR_YELLOW,
)

    @OptIn(ExperimentalForeignApi::class)
    fun processLogLine(
        pad: Pad,
        it: IndexedValue<LogLine>,
    ) {
        val fp = pad.fp
        //waddstr(fp, "${it.index} ")
        val logLine = it.value

        if (logLine is Original) {
            waddstr(fp, "${logLine.line}\n")

        } else if (logLine is Parsed) {
            printTag(pad, logLine.tag)

            waddstr(fp, " ")

            when (logLine.level) {
                "W" -> {
                    wattron(fp, COLOR_PAIR(6));
                    waddstr(fp, " ${logLine.level} ")
                    wattroff(fp, COLOR_PAIR(6));

                    waddstr(fp, " ")

                    wattron(fp, COLOR_PAIR(3))
                    waddstr(fp, wrapLine(pad, "${logLine.message}"))
                    wattroff(fp, COLOR_PAIR(3))
                }

                "E" -> {
                    wattron(fp, COLOR_PAIR(11))
                    waddstr(fp, " ${logLine.level} ")
                    wattroff(fp, COLOR_PAIR(11))

                    waddstr(fp, " ")

                    wattron(fp, COLOR_PAIR(1))
                    waddstr(fp, wrapLine(pad, "${logLine.message}"))
                    wattroff(fp, COLOR_PAIR(1));
                }

                "I" -> {
                    wattron(fp, COLOR_PAIR(12))
                    waddstr(fp, " ${logLine.level} ")
                    wattroff(fp, COLOR_PAIR(12))

                    waddstr(fp, " ")

                    wattron(fp, A_BOLD.toInt())
                    waddstr(fp, wrapLine(pad, "${logLine.message}"))
                    wattroff(fp, A_BOLD.toInt())
                }
                /*"F" -> {

                }*/

                else -> {
                    wattron(fp, COLOR_PAIR(12))
                    waddstr(fp, " ${logLine.level} ")
                    wattroff(fp, COLOR_PAIR(12))

                    waddstr(fp, " ")

                    //wattron(fp, A_DIM.toInt())
                    waddstr(fp, wrapLine(pad, "${logLine.message}"))
                    //wattroff(fp, A_DIM.toInt())
                }
            }
            //waddstr(fp, "${it.value.message} \n")
        }
        // yield()
    }

    fun wrapLine(
        pad: Pad,
        message: String
    ): String {
        val width = pad.position.endX
        val header = Config.tagWidth + 1 + 3 + 1// space, level, space
        val line = message.replace("\t", "    ") //prevent escape characters leaking
        val wrapArea = width - header
        var buf = ""
        var current = 0

        while (current < line.length) {
            val next = min(current + wrapArea, line.length)
            buf += line.substring(current, next)
            if (next < line.length) {
                //buf += "\n\r"
                buf += " ".repeat(header) //
            }
            current = next
        }
        return buf + "\n\r"
    }
}
