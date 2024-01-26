package di

import AppStateFlow
import BuildConfig
import FileLogger
import InternalAppStateFlow
import di.DogcatModule.dogcatModule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import logger.CanLog
import logger.NoOpLogger
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import ui.AppPresenter
import ui.logLines.LogLinesPresenter
import ui.status.StatusPresenter
import userInput.DefaultInput
import userInput.Input

class AppModule(
    private val uiDispatcher: CoroutineDispatcher
) {
    private val appModule = DI.Module("app") {
        bindSingleton<CanLog> {
            if (BuildConfig.DEBUG) {
                FileLogger()
            } else {
                NoOpLogger()
            }
        }
        bindSingleton<Input> { DefaultInput(Dispatchers.IO) }
        bindSingleton<AppStateFlow> { InternalAppStateFlow() }
        bindSingleton<AppPresenter> {
            AppPresenter(
                instance(),
                instance(),
                instance(),
                instance(),
                instance(),
            )
        }
        bindSingleton<StatusPresenter> { StatusPresenter(instance(), instance(), instance(), uiDispatcher) }
        bindSingleton<LogLinesPresenter> {
            LogLinesPresenter(
                instance(),
                instance(),
                instance(),
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
