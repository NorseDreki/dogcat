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
}