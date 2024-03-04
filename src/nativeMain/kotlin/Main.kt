import AppConfig.EXIT_CODE_ERROR
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import di.AppModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import platform.posix.exit
import userInput.AppArguments.ValidationException
import userInput.Keymap
import userInput.Keymap.Actions.QUIT

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    var exitCode = EXIT_CODE_ERROR

    val appModule = AppModule()
    Logger.set(appModule.fileLogger)

    try {
        appModule.appArguments.validate(args)
    } catch (e: ValidationException) {
        Logger.d("Application arguments validation failed: ${e.message}")

        println(e.message)
        exit(exitCode)
    }

    val handler = CoroutineExceptionHandler { _, e ->
        Logger.d("Exception in top-level handler: ${e.message}")

        runBlocking {
            appModule.appPresenter.stop()
        }

        println(e.message)
    }

    val ui = newSingleThreadContext("UI")

    runBlocking(ui) {
        val appJob = CoroutineScope(ui).launch(handler) {

            appModule.appPresenter.start()
            appModule.input.start()

            appModule.input
                .keypresses
                .filter {
                    Keymap.bindings[it] == QUIT
                }
                .onEach {
                    Logger.d("${context()} User quits the application")

                    appModule.appPresenter.stop()
                    coroutineContext.cancelChildren()

                    exitCode = 0
                }
                .launchIn(this@launch)
        }
        appJob.join()
    }

    ui.close()

    Logger.d("Exiting the application with code $exitCode")
    Logger.close()

    exit(exitCode)
}
