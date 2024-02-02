package di

import AdbShell
import AppStateFlow
import InternalAppStateFlow
import dogcat.*
import dogcat.state.DefaultAppliedFiltersState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

object DogcatModule {

    internal val dogcatModule = DI.Module("dogcat") {
        bindSingleton<DefaultAppliedFiltersState> { DefaultAppliedFiltersState() }
        bindSingleton<LogLineParser> { LogcatBriefParser() }
        bindSingleton<LogLines> { LogLines(instance(), instance(), instance(), Dispatchers.Default) }
        bindSingleton<Dogcat> { Dogcat(instance(), instance(), instance()) }
        bindSingleton<Shell> { AdbShell(Dispatchers.IO) }
    }
}
