import Arguments.current
import Arguments.packageName
import ServiceLocator.appStateFlow
import ServiceLocator.dogcat
import dogcat.Command.Start
import kotlinx.coroutines.*
import ui.DogcatPresenter
import ui.logLines.LogLinesPresenter
import ui.status.StatusPresenter

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    Arguments.validate(args)

    val ui = newSingleThreadContext("UI1")

    runBlocking(ui) {
        val input = DefaultInput(this, Dispatchers.IO)
        input.start()

        val app = DogcatPresenter(dogcat, appStateFlow, input, packageName, this)
        app.start()

        val status = StatusPresenter(dogcat, appStateFlow, input, this, ui)
        status.start()

        val logLines = LogLinesPresenter(dogcat, appStateFlow, input, this)
        logLines.start()

        when {
            packageName != null -> dogcat(Start.SelectAppByPackage(packageName!!))
            current == true -> dogcat(Start.SelectForegroundApp)
            else -> dogcat(Start.All)
        }
    }

    ui.close()
    Logger.close()

    //input.stop()
    //ncurses.end()
}
