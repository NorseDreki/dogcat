import di.AppModule
import kotlinx.coroutines.*
import logger.Logger
import userInput.Arguments

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    Arguments.validate(args)

    val ui = newSingleThreadContext("UI")

    val handler = CoroutineExceptionHandler { _, t ->
        Logger.d("!!!!!!11111111 CATCH! ${t.message}")

        //do we need to just terminate the app and write message to stdout on exit rather than logging?
    }

    runBlocking(ui + handler) {
        val appModule = AppModule(this)

        Logger.set(appModule.fileLogger)

        appModule.input.start()
        appModule.appPresenter.start()
    }

    Logger.d("Exit!")
    //ui.close()
    Logger.close()
    //input.stop()
}
