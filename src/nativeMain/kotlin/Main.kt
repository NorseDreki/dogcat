@file:OptIn(ExperimentalForeignApi::class)

import dogcat.*
import dogcat.Command.*
import dogcat.LogFilter.*
import dogcat.LogcatState.*
import kotlinx.cinterop.*
import kotlinx.cli.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ncurses.*
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import platform.LogcatSource
import platform.Logger

val dogcatModule = DI.Module("dogcat") {
    bindSingleton<InternalQuery> { InternalQuery() }
    bindSingleton<LogLinesSource> { LogcatSource(instance()) }
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

    val ui = newSingleThreadContext("UI")

    val ncurses = Ncurses()
    ncurses.start()

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)

    val padPosition = PadPosition(0, 5, sx, sy - 1)
    val pad = Pad(padPosition)

    val padPosition2 = PadPosition(0, 0, sx, 1)
    val pad2 = Pad(padPosition2)

    val lineColorizer = LogLineColorizer()
    val keymap = Keymap(this, pad, pad2)

    // legitimate use-case for 'GlobalScope'
    GlobalScope.launch {
        while (true) {
            val key = wgetch(stdscr)

            if (key == ERR) { //!= EOF //KEY_F(1)
                delay(50)
                continue
            }
            withContext(ui) {
                keymap.processInputKey(key)
            }
        }
    }

    runBlocking {



        launch(ui) {
            when {
                packageName != null -> dogcat(StartupAs.WithPackage(packageName!!))
                current == true -> dogcat(StartupAs.WithForegroundApp)
                else -> dogcat(StartupAs.All)
            }

            dogcat
                .state
                .filterIsInstance<CapturingInput>()
                .flatMapLatest { it.appliedFilters }
                .onEach {
                    Logger.d("Preparing to draw applied filters: $it")
                    wmove(pad2.fp, 0, 0)
                    wattron(pad2.fp, COLOR_PAIR(12))
                    wclrtoeol(pad2.fp)

                    it.forEach {
                        when (it.key) {
                            Substring::class -> {
                                mvwprintw(pad2.fp, 0, 0, "Filter by: ${(it.value.first as Substring).substring}")
                            }
                            MinLogLevel::class -> {
                                mvwprintw(pad2.fp, 0, 30, "${(it.value.first as MinLogLevel).logLevel} and up")
                            }
                            ByPackage::class -> {
                                mvwprintw(pad2.fp, 0, 80, "${(it.value.first as ByPackage).packageName} on")
                            }
                        }
                    }
                    prefresh(pad2.fp, 0, 0, 0, 0, 2, sx);

                    wattroff(pad2.fp, COLOR_PAIR(12))
                    pad.refresh()
                    yield()
                }
                .collect()
        }

        launch(ui) {
            dogcat
                .state
                .flatMapLatest {
                    when (it) {
                        is WaitingInput -> {
                            Logger.d("Waiting for log lines...\r")

                            emptyFlow()
                        }

                        is CapturingInput -> {
                            it.lines//.take(10)
                        }

                        InputCleared -> {
                            Logger.d("Cleared Logcat and re-started\r")
                            pad.clear()

                            emptyFlow()
                        }

                        Terminated -> {
                            cancel()
                            Logger.d("No more reading lines, terminated\r")
                            emptyFlow()
                        }
                    }
                }
                .onEach {
                    //pad.recordLine()
                    //Logger.d("${it.index} ${it.value} \r\n")
                    lineColorizer.processLogLine(pad, it)
                    pad.refresh()
                }
                .collect()
        }
    }

    ui.close()
    Logger.close()
}
