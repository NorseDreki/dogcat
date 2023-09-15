import dogcat.LogcatState.*
import dogcat.Dogcat
import dogcat.InternalState
import dogcat.LogSource
import dogcat.StartupAs
import kotlinx.cinterop.*
import kotlinx.cli.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ncurses.*
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import platform.LogcatSource

val dogcatModule = DI.Module("dogcat") {
    bindSingleton<InternalState> { InternalState() }
    bindSingleton<LogSource> { LogcatSource(instance()) }
    bindSingleton<Dogcat> { Dogcat(instance(), instance()) }
}
val di = DI {
    import(dogcatModule)
}
val dogcat: Dogcat by di.instance()

@OptIn(
    ExperimentalForeignApi::class,
    ExperimentalCoroutinesApi::class,
    DelicateCoroutinesApi::class
)
fun main(args: Array<String>): Unit = memScoped {
    val parser = ArgParser("dogcat")
    val packageName by parser.argument(ArgType.String, "package name", "description for p n").optional()
    val current by parser.option(ArgType.Boolean, shortName = "c", description = "Filter by currently running program")

    parser.parse(args)
    if (packageName != null && current != null) {
        //can't have both at the same time
    }

    val ncurses = Ncurses()
    ncurses.start()

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)

    val padPosition = PadPosition(0, 5, sx, sy - 1)
    val pad = Pad(padPosition)

    val lineColorizer = LogLineColorizer()
    val keymap = Keymap(this, pad, "")

    // legitimate use-case for 'GlobalScope'
    GlobalScope.launch {
        while (true) {
            val key = wgetch(stdscr)

            if (key == ERR) { //!= EOF //KEY_F(1)
                delay(50)
                continue
            }
            keymap.processInputKey(key)
        }
    }

    runBlocking {
        when {
            packageName != null -> dogcat(StartupAs.WithPackage(packageName!!))
            current == true -> dogcat(StartupAs.WithForegroundApp)
            else -> dogcat(StartupAs.All)
        }

        launch {
            dogcat
                .state
                .filterIsInstance<CapturingInput>()
                .flatMapLatest { it.appliedFilters }
                .onEach {
                    mvwprintw(stdscr, 0, 0, "--> $it")
                    wrefresh(stdscr)
                }
                .collect()
        }

        dogcat
            .state
            .flatMapLatest {
                when (it) {
                    is WaitingInput -> {
                        println("Waiting for log lines...\r")
                        emptyFlow()
                    }
                    is CapturingInput -> {
                        pad.clear()
                        it.lines
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
                lineColorizer.processLogLine(pad, it)
                pad.refresh()
            }
            .collect()
    }
}
