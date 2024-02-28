import di.AppModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logger.Logger
import logger.context
import platform.posix.exit
import userInput.Arguments
import userInput.Keymap
import userInput.Keymap.Actions

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    val appModule = AppModule()

    try {
        appModule.arguments.validate(args)
    } catch (e: Arguments.ValidationException) {
        println(e.message)
        exit(1)
    }

    val ui = newSingleThreadContext("UI")

    val handler = CoroutineExceptionHandler { c, t ->
        Logger.d("TOP LEVEL CATCH! $t ${t.message}")

        /*runBlocking {
            appModule.appPresenter.stop()
        }*/

        println("${t.message}")
    }

    runBlocking(ui) {
        //he key takeaway is that if you call launch on a custom CoroutineScope, any CoroutineExceptionHandler provided
        // directly to the CoroutineScope constructor or to launch will be executed when an exception is thrown within the launched coroutine.
        val appJob = CoroutineScope(ui).launch(handler) {
            with(appModule) {
                Logger.set(fileLogger)

                appPresenter.start()
                input.start()

                input
                    .keypresses
                    .filter {
                        Keymap.bindings[it] == Actions.QUIT
                    }
                    .onEach {
                        Logger.d("${context()} Cancel scope")

                        appPresenter.stop()
                        coroutineContext.cancelChildren()
                    }
                    .launchIn(this@launch)
            }
        }
        appJob.join()
    }



    ui.close()
    //close presenter
    //exit with nonzero upon exception

    Logger.d("Exit!")
    Logger.close()
}
