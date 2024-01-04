import Arguments.current
import Arguments.packageName
import ServiceLocator.appStateFlow
import ServiceLocator.dogcat
import dogcat.Command.Start
import dogcat.PublicState.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ncurses.*
import ui.*
import ui.logLines.LogLinesView
import ui.logLines.PadPosition
import ui.logLines.processLogLine
import ui.status.StatusPresenter

@OptIn(
    ExperimentalForeignApi::class,
    ExperimentalCoroutinesApi::class,
    DelicateCoroutinesApi::class,
)
fun main(args: Array<String>): Unit = memScoped {
    Arguments.validate(args)

    val ui = newSingleThreadContext("UI1")

    val ncurses = DogcatPresenter()
    ncurses.start()

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)

    echo()
    /*val fp = newwin(0,0,sy - 2, 0)
    wmove(fp, 0, 0)
    wattron(fp, COLOR_PAIR(12))

    waddstr(fp, "2".repeat(sx))*/
    //wrefresh(fp)

    //noraw()

    val padPosition = PadPosition(0, 0, sx, sy - 5)
    val pad = LogLinesView(padPosition)

    val padPosition2 = PadPosition(0, sy - 2, sx, sy - 1)
    val pad2 = LogLinesView(padPosition2, 2, true)

    val keymap = Keymap(  this, pad, pad2, packageName)



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

        val input = DefaultInput(Dispatchers.IO)
        val status = StatusPresenter(dogcat, appStateFlow, input, this)
        status.start()

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
