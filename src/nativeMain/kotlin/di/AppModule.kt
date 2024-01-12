package di

import AdbShell
import AppStateFlow
import FileLogger
import InternalAppStateFlow
import di.DogcatModule.dogcatModule
import dogcat.*
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import dogcat.LogcatBriefParser
import dogcat.state.DefaultAppliedFiltersState
import kotlinx.coroutines.*
import logger.CanLog
import logger.Logger
import ui.AppPresenter
import userInput.DefaultInput
import userInput.Input

object AppModule {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val ui = newSingleThreadContext("UI2")

    val handler = CoroutineExceptionHandler { _, t -> Logger.d("!!!!!!11111111 CATCH! ${t.message}\r") }

    val scope11 = CoroutineScope(ui + handler + Job())

    private val appModule = DI.Module("app") {
        bindSingleton<CanLog> { FileLogger() }
        bindSingleton<Input> { DefaultInput(scope11, Dispatchers.IO) }
        bindSingleton<AppStateFlow> { InternalAppStateFlow() }
        bindSingleton<AppPresenter> { AppPresenter(instance(), instance(), instance(), scope11) }
    }

    private val serviceLocator = DI {
        import(dogcatModule)
        import(appModule)
    }

    val fileLogger: CanLog by serviceLocator.instance()

    val input: Input by serviceLocator.instance()

    val appPresenter: AppPresenter by serviceLocator.instance()

    //encapsulate
    val dogcat: Dogcat by serviceLocator.instance()

    val appStateFlow: AppStateFlow by serviceLocator.instance()
}
