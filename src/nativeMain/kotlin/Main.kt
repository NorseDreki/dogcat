import Arguments.current
import Arguments.packageName
import ServiceLocator.dogcat
import dogcat.Command.Start
import dogcat.PublicState.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ncurses.*
import ui.Pad
import ui.PadPosition
import ui.printStatusLine
import ui.processLogLine

@OptIn(
    ExperimentalForeignApi::class,
    ExperimentalCoroutinesApi::class,
    DelicateCoroutinesApi::class,
)
fun main(args: Array<String>): Unit = memScoped {
    Arguments.validate(args)

    val ui = newSingleThreadContext("UI1")

    val ncurses = Ncurses()
    ncurses.start()

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)

    //noraw()

    val padPosition = PadPosition(0, 3, sx, sy)
    val pad = Pad(padPosition)

    val padPosition2 = PadPosition(0, 0, sx, 1)
    val pad2 = Pad(padPosition2, 1)

    val keymap = Keymap(this, pad, pad2, packageName)



    runBlocking(ui) {
        launch(Dispatchers.IO) {
            while (true) {
                val key = wgetch(stdscr)

                if (key == ERR) { //!= EOF
                    delay(30)
                    continue
                }

                withContext(ui) {
                    keymap.processInputKey(key)
                }
            }
        }

        dogcat
            .state
            .filterIsInstance<CapturingInput>()
            .flatMapLatest { it.applied }
            .onEach { pad2.printStatusLine(it) }
            .launchIn(this)

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
                        //cancel()
                        Logger.d("No more reading lines, terminated\r")
                        emptyFlow()
                    }
                }
            }
            .onEach { pad.processLogLine(it) }
            .launchIn(this)

        when {
            packageName != null -> dogcat(Start.SelectAppByPackage(packageName!!))
            current == true -> dogcat(Start.SelectForegroundApp)
            else -> dogcat(Start.All)
        }
    }

    ui.close()
    Logger.close()

    //ncurses.end()
}
