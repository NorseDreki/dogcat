package di

import AppStateFlow
import FileLogger
import InternalAppStateFlow
import di.DogcatModule.dogcatModule
import kotlinx.coroutines.*
import logger.CanLog
import logger.Logger
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import ui.AppPresenter
import ui.logLines.LogLinesPresenter
import ui.status.StatusPresenter
import userInput.DefaultInput
import userInput.Input

object AppModule {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val ui = newSingleThreadContext("UI2")

    val handler = CoroutineExceptionHandler { _, t -> Logger.d("!!!!!!11111111 CATCH! ${t.message}\r") }

    //val scope11 = CoroutineScope(ui + handler + Job())
    var scope11 = CoroutineScope(ui + handler + Job())

    private val appModule = DI.Module("app") {
        bindSingleton<CanLog> { FileLogger() }
        bindSingleton<Input> { DefaultInput(scope11, Dispatchers.IO) }
        bindSingleton<AppStateFlow> { InternalAppStateFlow() }
        bindSingleton<AppPresenter> {
            AppPresenter(
                instance(),
                instance(),
                instance(),
                instance(),
                instance(),
                scope11
            )
        }
        bindSingleton<StatusPresenter> { StatusPresenter(instance(), instance(), instance(), scope11, ui) }
        bindSingleton<LogLinesPresenter> { LogLinesPresenter(instance(), instance(), instance(), scope11, ui) }
    }

    private val serviceLocator = DI {
        import(dogcatModule)
        import(appModule)
    }

    val fileLogger: CanLog by serviceLocator.instance()

    val input: Input by serviceLocator.instance()

    val appPresenter: AppPresenter by serviceLocator.instance()

    //encapsulate
    val appStateFlow: AppStateFlow by serviceLocator.instance()
}
