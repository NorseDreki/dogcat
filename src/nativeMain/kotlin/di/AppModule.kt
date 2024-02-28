package di

import AppState
import BuildConfig
import FileLogger
import InternalAppState
import di.DogcatModule.dogcatModule
import kotlinx.cli.ArgParser
import com.norsedreki.logger.CanLog
import com.norsedreki.logger.NoOpLogger
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import ui.AppPresenter
import ui.logLines.LogLinesPresenter
import ui.status.StatusPresenter
import userInput.AppArguments
import userInput.DefaultInput
import userInput.Input

class AppModule {

    private val appModule = DI.Module("app") {
        bindSingleton<CanLog> {
            if (BuildConfig.DEBUG) {
                FileLogger()
            } else {
                NoOpLogger()
            }
        }
        bindSingleton<AppArguments> { AppArguments(ArgParser("dogcat")) }
        bindSingleton<AppState> { InternalAppState() }
        bindSingleton<Input> { DefaultInput(instance()) }
        bindSingleton<AppPresenter> {
            AppPresenter(
                instance(),
                instance(),
                instance(),
                instance(),
                instance(),
                instance()
            )
        }
        bindSingleton<StatusPresenter> {
            StatusPresenter(
                instance(),
                instance(),
                instance()
            )
        }
        bindSingleton<LogLinesPresenter> {
            LogLinesPresenter(
                instance(),
                instance(),
                instance(),
                instance()
            )
        }
    }

    private val serviceLocator = DI {
        import(dogcatModule)
        import(appModule)
    }

    val fileLogger: CanLog by serviceLocator.instance()

    val appArguments: AppArguments by serviceLocator.instance()

    val input: Input by serviceLocator.instance()

    val appPresenter: AppPresenter by serviceLocator.instance()
}
