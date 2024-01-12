package di

import AdbShell
import AppStateFlow
import InternalAppStateFlow
import dogcat.*
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import dogcat.LogcatBriefParser
import dogcat.state.DefaultAppliedFiltersState

object DogcatModule {

    private val dogcatModule = DI.Module("dogcat") {
        bindSingleton<DefaultAppliedFiltersState> { DefaultAppliedFiltersState() }
        bindSingleton<Shell> { AdbShell() }
        bindSingleton<LogLineParser> { LogcatBriefParser() }
        bindSingleton<LogLines> { LogLines(instance(), instance(), instance()) }
        bindSingleton<Dogcat> { Dogcat(instance(), instance(), instance()) }
        bindSingleton<AppStateFlow> { InternalAppStateFlow() }
    }

    private val serviceLocator = DI {
        import(dogcatModule)
    }

    val dogcat: Dogcat by serviceLocator.instance()

    val appStateFlow: AppStateFlow by serviceLocator.instance()
}
