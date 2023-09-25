import dogcat.*
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import platform.LogcatBriefParser
import platform.LogcatSource

object ServiceLocator {

    private val dogcatModule = DI.Module("dogcat") {
        bindSingleton<InternalAppliedFiltersState> { InternalAppliedFiltersState() }
        bindSingleton<LogLinesSource> { LogcatSource(instance()) }
        bindSingleton<LogLineParser> { LogcatBriefParser() }
        bindSingleton<LogLines> { LogLines(instance(), instance(), instance()) }
        bindSingleton<Dogcat> { Dogcat(instance(), instance()) }
    }

    private val serviceLocator = DI {
        import(dogcatModule)
    }

    val dogcat: Dogcat by serviceLocator.instance()
}
