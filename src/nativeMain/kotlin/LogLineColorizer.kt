import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*

class LogLineColorizer {
    @OptIn(ExperimentalForeignApi::class)
    fun processLogLine(
        fp: CPointer<WINDOW>?,
        it: IndexedValue<LogLine>,
    ) {
        //waddstr(fp, "${it.index} ")
        val logLine = it.value

        if (logLine is Original) {
            waddstr(fp, "${logLine.line}\n")

        } else if (logLine is Parsed) {

            waddstr(fp, "${logLine.tag} ")
            /*wattron(fp, COLOR_PAIR(1));
                    waddstr(fp, "${it.value.tag} ||")
                    //wprintw(fp, it.value)
                    wattroff(fp, COLOR_PAIR(1))*/

            //wattron(fp, COLOR_PAIR(2));
            //waddstr(fp, "${logLine.owner} ||")
            //wattroff(fp, COLOR_PAIR(2));

            when (logLine.level) {
                "W" -> {
                    wattron(fp, COLOR_PAIR(3));
                    waddstr(fp, "[${logLine.level}] ")
                    waddstr(fp, "${logLine.message}\n")
                    wattroff(fp, COLOR_PAIR(3));
                }

                "E" -> {
                    wattron(fp, COLOR_PAIR(1));
                    waddstr(fp, "[${logLine.level}] ")
                    waddstr(fp, "${logLine.message}\n")
                    wattroff(fp, COLOR_PAIR(1));
                }

                "I" -> {
                    //wattron(fp, COLOR_PAIR(4));
                    waddstr(fp, "[${logLine.level}] ")
                    waddstr(fp, "${logLine.message}\n")
                    //wattroff(fp, COLOR_PAIR(4));
                }

                else -> {
                    waddstr(fp, "[${logLine.level}] ")
                    waddstr(fp, "${logLine.message}\n")
                }
            }
            //waddstr(fp, "${it.value.message} \n")
        }
        // yield()
    }

    /*
    def indent_wrap(message):
  if width == -1:
    return message
  message = message.replace('\t', '    ')
  wrap_area = width - header_size
  messagebuf = ''
  current = 0
  while current < len(message):
    next = min(current + wrap_area, len(message))
    messagebuf += message[current:next]
    if next < len(message):
      messagebuf += '\n'
      messagebuf += ' ' * header_size
    current = next
  return messagebuf
*/

    fun wrapLine(
        pad: Pad,
        message: String
    ): String {
        val width = pad.position.endX
        val header = Config.tagWidth + 1 + 3 + 1// space, level, space
        val line = message.replace("\t", "    ")
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