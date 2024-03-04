import AppConfig.EXIT_CODE_ERROR
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import di.AppModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import platform.posix.exit
import userInput.AppArguments.ValidationException
import userInput.Keymap
import userInput.Keymap.Actions.QUIT

val appModule = AppModule()

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
val ui = newSingleThreadContext("UI")

lateinit var appJob: Job

fun main(args: Array<String>) {
    Logger.set(appModule.fileLogger)

    try {
        appModule.appArguments.validate(args)
    } catch (e: ValidationException) {
        Logger.d("Application arguments validation failed: ${e.message}")

        stop(EXIT_CODE_ERROR, e.message)
    }

    val handler = CoroutineExceptionHandler { _, t ->
        Logger.d("Exception in top-level handler: ${t.message}")

        stop(EXIT_CODE_ERROR, t.message)

        Logger.d("Exit handler")
    }



    runBlocking(ui) {
        appJob = CoroutineScope(ui).launch(handler) {

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
                }
                .onCompletion {
                    Logger.d("Inpt compl")
                }
                .launchIn(this@launch)

            Logger.d("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        }
        appJob.join()
        Logger.d("Joined app job")
    }

    Logger.d("BEFORE STOP")

    stop(0)

    Logger.d("AFTER STOP")
    ui.close()
    Logger.d("UI.close complete")

    exit(0)
}


fun stop(exitCode: Int, exitMessage: String? = null) {
    Logger.d("EXIT APP WITH STOP")

    runBlocking {
        appModule.appPresenter.stop()
    }

    //appJob.cancel()

    Logger.d("Close ui, ${appJob.isActive} ${appJob.isCancelled}, ${appJob.isCompleted}")

    ui.close()

    Logger.d("Exit the application with code $exitCode")
    //Logger.close()

    exitMessage?.let {
        println(it)
    }

    //exit(exitCode)
}
