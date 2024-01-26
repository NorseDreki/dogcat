import di.AppModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logger.Logger
import logger.context
import platform.posix.signal
import userInput.Arguments
import userInput.Keymap
import userInput.Keymap.Actions

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    Arguments.validate(args)

    val ui = newSingleThreadContext("UI")

    val handler = CoroutineExceptionHandler { _, t ->
        Logger.d("!!!!!!11111111 CATCH! ${t.message}")
        //do we need to just terminate the app and write message to stdout on exit rather than logging?
    }

    runBlocking(ui) {
        val appModule = AppModule(ui)

        with(appModule) {
            Logger.set(fileLogger)

            input.start()
            appPresenter.start()

            input
                .keypresses
                .filter {
                    Keymap.bindings[it] == Actions.Quit
                }
                .onEach {
                    Logger.d("${context()} Cancel scope")

                    //make sure to cancel last leaking ADB
                    appPresenter.stop()
                    coroutineContext.cancelChildren()
                }
                .launchIn(this@runBlocking)
        }
    }
    ui.close()

    Logger.d("Exit!")
    Logger.close()

    /*signal(SIGINT, staticCFunction<Int, Unit> { signal ->
        // This code will be executed when Ctrl+C is pressed
        println("SIGINT received")
        exitProcess(0)
    })*/
}
