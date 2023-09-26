import Arguments.current
import Arguments.packageName
import ServiceLocator.dogcat
import dogcat.*
import dogcat.Command.*
import dogcat.LogFilter.*
import dogcat.PublicState.*
import kotlinx.cinterop.*
import kotlinx.cli.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ncurses.*
import platform.Logger

@OptIn(
    ExperimentalForeignApi::class,
    ExperimentalCoroutinesApi::class,
    DelicateCoroutinesApi::class
)
fun main(args: Array<String>) = memScoped {
    Arguments.validate(args)

    val ui = newSingleThreadContext("UI")

    val ncurses = Ncurses()
    ncurses.start()

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)

    val padPosition = PadPosition(0, 5, sx, sy - 1)
    val pad = Pad(padPosition)

    val padPosition2 = PadPosition(0, 0, sx, 1)
    val pad2 = Pad(padPosition2)

    val keymap = Keymap(this, pad, pad2, packageName)

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
                packageName != null -> dogcat(Start.SelectAppByPackage(packageName!!))
                current == true -> dogcat(Start.SelectForegroundApp)
                else -> dogcat(Start.All)
            }

            dogcat
                .state
                .filterIsInstance<CapturingInput>()
                .onEach {
                    //pad2.dn()
                }
                .flatMapLatest { it.applied }
                .onEach {
                    pad2.printStatusLine(it)
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
                        Stopped -> {
                            cancel()
                            Logger.d("No more reading lines, terminated\r")
                            emptyFlow()
                        }
                    }
                }
                .onEach { pad.processLogLine(it) }
                .collect()
        }
    }
    ui.close()
    Logger.close()
}
