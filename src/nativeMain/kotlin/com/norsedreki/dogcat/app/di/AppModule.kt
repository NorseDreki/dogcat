package com.norsedreki.dogcat.app.di

import com.norsedreki.dogcat.app.*
import com.norsedreki.dogcat.app.di.DogcatModule.dogcatModule
import com.norsedreki.dogcat.app.ui.AppPresenter
import com.norsedreki.dogcat.app.ui.DefaultInput
import com.norsedreki.dogcat.app.ui.Input
import com.norsedreki.dogcat.app.ui.logLines.LogLinesPresenter
import com.norsedreki.dogcat.app.ui.status.StatusPresenter
import com.norsedreki.logger.CanLog
import com.norsedreki.logger.NoOpLogger
import kotlinx.cli.ArgParser
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

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
