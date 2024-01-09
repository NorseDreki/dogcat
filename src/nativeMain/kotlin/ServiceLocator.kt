import dogcat.*
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import dogcat.LogcatBriefParser

object ServiceLocator {

    private val dogcatModule = DI.Module("dogcat") {
        bindSingleton<InternalAppliedFiltersState> { InternalAppliedFiltersState() }
        bindSingleton<Environment> { AdbEnvironment() }
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

    val environment: Environment by serviceLocator.instance()
}
