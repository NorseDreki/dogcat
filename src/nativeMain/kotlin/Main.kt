import Arguments.current
import Arguments.packageName
import DogcatModule.appStateFlow
import DogcatModule.dogcat
import dogcat.Command.Start
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.*
import logger.Logger
import platform.posix.getenv
import ui.AppPresenter
import ui.logLines.LogLinesPresenter
import ui.status.StatusPresenter

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class, ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    Arguments.validate(args)

    val ui = newSingleThreadContext("UI1")

    runBlocking(ui) {
        val variable = "whatever..."



        println(getenv(variable)?.toKString())


        val logger = FileLogger()
        Logger.set(logger)

        val input = DefaultInput(this, Dispatchers.IO)
        input.start()

        val app = AppPresenter(dogcat, appStateFlow, input, this)
        app.start()

        val status = StatusPresenter(dogcat, appStateFlow, input, this, ui)
        status.start()

        val logLines = LogLinesPresenter(dogcat, appStateFlow, input, this, ui)
        logLines.start()

        when {
            packageName != null -> dogcat(Start.PickApp(packageName!!))
            current == true -> dogcat(Start.PickForegroundApp)
            else -> dogcat(Start.All)
        }
    }

    ui.close()
    Logger.close()

    //input.stop()
    //ncurses.end()
}
