import LogcatState.CapturingInput
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ncurses.*
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import platform.posix.LC_ALL
import platform.posix.exit
import platform.posix.printf
import platform.posix.setlocale

val dogcatModule = DI.Module("dogcat") {
    bindSingleton<LogSource> { LogcatSource() }
    bindSingleton<Logcat> { Logcat(instance()) }
}

val di = DI {
    import(dogcatModule)
}

val logcat: Logcat by di.instance()

@OptIn(ExperimentalForeignApi::class, ExperimentalCoroutinesApi::class)
fun main(): Unit = memScoped {
    setlocale(LC_ALL, "en_US.UTF-8")
    initscr()
    intrflush(stdscr, false)
    savetty()
    noecho()
    //cbreak() //making getch() work without a buffer I.E. raw characters
    keypad(stdscr, true) //allows use of special keys, namely the arrow keys
    clear()

    if (!has_colors()) {
        endwin()
        printf("Your terminal does not support color\n")
        exit(1)
    }
    use_default_colors()
    start_color()
    init_pair(1, COLOR_RED.toShort(), COLOR_BLACK.toShort())
    init_pair(2, COLOR_GREEN.toShort(), COLOR_BLACK.toShort())
    init_pair(3, COLOR_YELLOW.toShort(), COLOR_BLACK.toShort())
    init_pair(4, COLOR_CYAN.toShort(), COLOR_BLACK.toShort())

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)
    val fp = newpad(Config.LogLinesBufferCount, sx)
    scrollok(fp, true)
    //keypad(fp, true)

    runBlocking {
        var a = 25;

        logcat
            .state
            .filterIsInstance<CapturingInput>()
            .flatMapMerge { it.lines }
            .withIndex()
            .onEach {
                processLogLine(fp, it, sy, sx, a)
            }
            .launchIn(this)

        launch(Dispatchers.Default) {
            logcat(StartupAs.All)

            while (true) {
                var key = wgetch(stdscr);

                processInputKey(key, stdscr, sx, sy, fp, a)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun MemScope.processInputKey(
    key: Int,
    stdscr: CPointer<WINDOW>?,
    sx: Int,
    sy: Int,
    fp: CPointer<WINDOW>?,
    a: Int
) {
    var a1 = a
    when (key) {
        'f'.code -> {
            //wprintw(stdscr, name)
            //println(name)
            mvwprintw(stdscr, 0, 0, "$sx:$sy")
            //mvwprintw(stdscr, 0, 0, name)
            clrtoeol()
            echo()

            val bytePtr = allocArray<ByteVar>(200)
            getnstr(bytePtr, 200)
            noecho()
            wclear(fp)

            logcat(Filter.ByString(bytePtr.toKString()))
            yield()
        }

        'q'.code -> {
            delwin(fp); exit(0)
        }

        'a'.code -> {
            a1 = 0
            prefresh(fp, 0, 0, 3, 0, sy - 1, sx);
        }

        'z'.code -> {
            /*a = 0
                        prefresh(fp, 0, 0, 3,0, sy-1, sx);*/
        }

        'w'.code -> {
            a1--
            prefresh(fp, a1, 0, 3, 0, sy - 1, sx);
        }

        's'.code -> {
            a1++
            prefresh(fp, a1, 0, 3, 0, sy - 1, sx);
        }

        'd'.code -> {
            a1 += sy - 1 - 3
            prefresh(fp, a1, 0, 3, 0, sy - 1, sx);
        }

        'e'.code -> {
            a1 -= sy - 1 - 3
            prefresh(fp, a1, 0, 3, 0, sy - 1, sx);
        }

        '6'.code -> {
            logcat(Filter.ToggleLogLevel("V"))
        }

        '7'.code -> {
            logcat(Filter.ToggleLogLevel("D"))
        }

        '8'.code -> {
            logcat(Filter.ToggleLogLevel("I"))
        }

        '9'.code -> {
            logcat(Filter.ToggleLogLevel("W"))
        }

        '0'.code -> {
            logcat(Filter.ToggleLogLevel("E"))
        }

        'c'.code -> {
            logcat(ClearLogs)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun processLogLine(
    fp: CPointer<WINDOW>?,
    it: IndexedValue<LogLine>,
    sy: Int,
    sx: Int,
    a: Int
) {
    //println("${it.index} ${it.value} \r\n")
    waddstr(fp, "${it.index} ")
    val logLine = it.value

    if (logLine is Original) {
        waddstr(fp, "${logLine}\n")

    } else if (logLine is Parsed) {

        waddstr(fp, "${logLine.tag} ||")
        /*wattron(fp, COLOR_PAIR(1));
                waddstr(fp, "${it.value.tag} ||")
                //wprintw(fp, it.value)
                wattroff(fp, COLOR_PAIR(1))*/

        //wattron(fp, COLOR_PAIR(2));
        waddstr(fp, "${logLine.owner} ||")
        //wattroff(fp, COLOR_PAIR(2));

        when (logLine.level) {
            "W" -> {
                wattron(fp, COLOR_PAIR(3));
                waddstr(fp, "${logLine.level} ||")
                waddstr(fp, "${logLine.message} ||\n")
                wattroff(fp, COLOR_PAIR(3));
            }

            "E" -> {
                wattron(fp, COLOR_PAIR(1));
                waddstr(fp, "${logLine.level} ||")
                waddstr(fp, "${logLine.message} ||\n")
                wattroff(fp, COLOR_PAIR(1));
            }

            "I" -> {
                wattron(fp, COLOR_PAIR(4));
                waddstr(fp, "${logLine.level} ||")
                waddstr(fp, "${logLine.message} ||\n")
                wattroff(fp, COLOR_PAIR(4));
            }

            else -> {
                waddstr(fp, "${logLine.level} ||")
                waddstr(fp, "${logLine.message} ||\n")
            }
        }
        //waddstr(fp, "${it.value.message} \n")
    }

    yield()
}
