package di

import AdbShell
import AppStateFlow
import InternalAppStateFlow
import di.DogcatModule.dogcatModule
import dogcat.*
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import dogcat.LogcatBriefParser
import dogcat.state.DefaultAppliedFiltersState

object AppModule {

    private val appModule = DI.Module("app") {
        bindSingleton<AppStateFlow> { InternalAppStateFlow() }
    }

    private val serviceLocator = DI {
        import(dogcatModule)
        import(appModule)
    }

    //encapsulate
    val dogcat: Dogcat by serviceLocator.instance()

    val appStateFlow: AppStateFlow by serviceLocator.instance()
}
