package di

import AdbShell
import com.norsedreki.dogcat.*
import com.norsedreki.dogcat.state.AppliedFiltersState
import com.norsedreki.dogcat.state.DefaultAppliedFiltersState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

object DogcatModule {

    internal val dogcatModule = DI.Module("com/norsedreki/dogcat") {
        bindSingleton<AppliedFiltersState> { DefaultAppliedFiltersState() }
        bindSingleton<LogLineParser> { LogLineBriefParser() }
        bindSingleton<LogLines> {
            LogLines(
                instance(),
                instance(),
                instance(),
                Dispatchers.Default
            )
        }
        bindSingleton<Shell> { AdbShell(Dispatchers.IO) }
        bindSingleton<Dogcat> {
            Dogcat(
                instance(),
                instance(),
                instance()
            )
        }
    }
}
