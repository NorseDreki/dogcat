import di.AppModule
import di.AppModule.appPresenter
import di.AppModule.fileLogger
import di.AppModule.input
import kotlinx.coroutines.runBlocking
import logger.Logger
import userInput.Arguments

fun main(args: Array<String>) {
    Arguments.validate(args)
    //val ui = newSingleThreadContext("UI1")

    runBlocking(AppModule.ui) {
        Logger.set(fileLogger)
        AppModule.scope11 = this

        input.start()
        appPresenter.start()
    }
    Logger.d("Exit!")

    //ui.close()
    Logger.close()
    //input.stop()
}
