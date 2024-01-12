import di.AppModule.appPresenter
import di.AppModule.appStateFlow
import di.AppModule.dogcat
import di.AppModule.fileLogger
import di.AppModule.input
import dogcat.Command.Start
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import logger.Logger
import ui.logLines.LogLinesPresenter
import ui.status.StatusPresenter
import userInput.Arguments
import userInput.Arguments.current
import userInput.Arguments.packageName

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class, ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    Arguments.validate(args)

    val ui = newSingleThreadContext("UI1")

    runBlocking(ui) {
        Logger.set(fileLogger)

        input.start()

        appPresenter.start()

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
}
