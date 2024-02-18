import di.AppModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logger.Logger
import logger.context
import userInput.Arguments
import userInput.Keymap
import userInput.Keymap.Actions

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    Arguments.validate(args)

    val ui = newSingleThreadContext("UI")

    val handler = CoroutineExceptionHandler { c, t ->
        Logger.d("CATCH! ${t.message}")
        println("${t.message}")
    }

    runBlocking(ui) {
        //he key takeaway is that if you call launch on a custom CoroutineScope, any CoroutineExceptionHandler provided
        // directly to the CoroutineScope constructor or to launch will be executed when an exception is thrown within the launched coroutine.
        val appJob = CoroutineScope(ui).launch(handler) {
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

                        appPresenter.stop()
                        coroutineContext.cancelChildren()
                    }
                    .launchIn(this@launch)
            }
        }
        appJob.join()
    }
    ui.close()

    Logger.d("Exit!")
    Logger.close()
}
