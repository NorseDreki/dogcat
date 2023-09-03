import LogcatState.*
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
    bindSingleton<Dogcat> { Dogcat(instance()) }
}

val di = DI {
    import(dogcatModule)
}

val dogcat: Dogcat by di.instance()

@OptIn(ExperimentalForeignApi::class, ExperimentalCoroutinesApi::class)
fun main(): Unit = memScoped {
    setlocale(LC_ALL, "en_US.UTF-8")
    initscr()
    intrflush(stdscr, false)
    savetty()
    noecho()
    cbreak() //making getch() work without a buffer I.E. raw characters
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

    val padPosition = PadPosition(0, 5, sx, sy - 1)
    val pad = Pad(padPosition)

    val lineColorizer = LogLineColorizer()

    runBlocking {
        launch(Dispatchers.Default) {
            dogcat
                .state
                .flatMapLatest {
                    when (it) {
                        is WaitingInput -> {
                            println("Waiting for log lines...\r")
                            emptyFlow()
                        }

                        is CapturingInput -> {
                            println(">>>>>> NEXT Capturing...")
                            pad.clear()
                            it.lines.withIndex()
                        }

                        InputCleared -> {
                            println("Cleared Logcat and re-started\r")
                            emptyFlow()
                        }

                        Terminated -> {
                            cancel()
                            println("No more reading lines, terminated\r")
                            emptyFlow()
                        }
                    }
                }
                .onEach {
                    pad.recordLine()
                    //println("${it.index} ${it.value} \r\n")
                    lineColorizer.processLogLine(pad.fp, it)
                    pad.refresh()
                }
                .collect()
        }

        //what if there is only one thread at runtime?
        launch(Dispatchers.Default) {
            while (true) {
                //Maybe change to non-blocking reading and use Main instead
                // Read char and amke a delay
                val key = wgetch(stdscr);

                processInputKey(key, pad)
            }
        }

        dogcat(StartupAs.All)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun MemScope.processInputKey(
    key: Int,
    pad: Pad
) {
    when (key) {
        'f'.code -> {
            mvwprintw(stdscr, 0, 0, ":")
            clrtoeol()
            echo()

            val bytePtr = allocArray<ByteVar>(200)
            getnstr(bytePtr, 200)
            noecho()
            pad.clear()

            dogcat(Filter.ByString(bytePtr.toKString()))
        }

        'q'.code -> {
            dogcat(StopEverything)
            pad.terminate()
            exit(0)
        }

        'a'.code -> pad.home()

        'z'.code -> pad.end()

        'w'.code -> pad.lineUp()

        's'.code -> pad.lineDown()

        'd'.code -> pad.pageDown()

        'e'.code -> pad.pageUp()

        '6'.code -> {
            dogcat(Filter.ToggleLogLevel("V"))
        }

        '7'.code -> {
            dogcat(Filter.ToggleLogLevel("D"))
        }

        '8'.code -> {
            dogcat(Filter.ToggleLogLevel("I"))
        }

        '9'.code -> {
            dogcat(Filter.ToggleLogLevel("W"))
        }

        '0'.code -> {
            dogcat(Filter.ToggleLogLevel("E"))
        }

        'c'.code -> {
            dogcat(ClearLogs)
        }
    }
}
