package di

import AppStateFlow
import FileLogger
import InternalAppStateFlow
import di.DogcatModule.dogcatModule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import logger.CanLog
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import ui.AppPresenter
import ui.logLines.LogLinesPresenter
import ui.status.StatusPresenter
import userInput.DefaultInput
import userInput.Input

@OptIn(ExperimentalStdlibApi::class)
class AppModule(
    private val uiScope: CoroutineScope
) {
    private val uiDispatcher = uiScope.coroutineContext[CoroutineDispatcher]!!

    private val appModule = DI.Module("app") {
        bindSingleton<CanLog> { FileLogger() }
        bindSingleton<Input> { DefaultInput(uiScope, Dispatchers.IO) }
        bindSingleton<AppStateFlow> { InternalAppStateFlow() }
        bindSingleton<AppPresenter> {
            AppPresenter(
                instance(),
                instance(),
                instance(),
                instance(),
                instance(),
                uiScope
            )
        }
        bindSingleton<StatusPresenter> { StatusPresenter(instance(), instance(), instance(), uiScope, uiDispatcher) }
        bindSingleton<LogLinesPresenter> {
            LogLinesPresenter(
                instance(),
                instance(),
                instance(),
                uiScope,
                uiDispatcher
            )
        }
    }

    private val serviceLocator = DI {
        import(dogcatModule)
        import(appModule)
    }

    val fileLogger: CanLog by serviceLocator.instance()

    val input: Input by serviceLocator.instance()

    val appPresenter: AppPresenter by serviceLocator.instance()
}
