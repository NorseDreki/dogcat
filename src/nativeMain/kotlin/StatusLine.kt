import dogcat.AppliedFilters
import dogcat.LogFilter
import dogcat.LogFilter.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.yield
import ncurses.*
import platform.Logger

@OptIn(ExperimentalForeignApi::class)
suspend fun Pad.printStatusLine(it: AppliedFilters) {
    val sx = getmaxx(stdscr)

    Logger.d("Preparing to draw applied filters: $it")
    wmove(fp, 0, 0)
    wattron(fp, COLOR_PAIR(12))
    wclrtoeol(fp)

    it.forEach {
        when (it.key) {
            Substring::class -> {
                mvwprintw(fp, 0, 0, "Filter by: ${(it.value.first as Substring).substring}")
            }
            MinLogLevel::class -> {
                mvwprintw(fp, 0, 30, "${(it.value.first as MinLogLevel).logLevel} and up")
            }
            ByPackage::class -> {
                mvwprintw(fp, 0, 80, "${(it.value.first as ByPackage).packageName} on")
            }
        }
    }
    prefresh(fp, 0, 0, 0, 0, 2, sx)

    wattroff(fp, COLOR_PAIR(12))
    refresh()
    yield()
}
