package com.norsedreki.dogcat.app.di

import com.norsedreki.dogcat.app.AdbShell
import com.norsedreki.dogcat.*
import com.norsedreki.dogcat.state.LogFiltersState
import com.norsedreki.dogcat.state.DefaultLogFiltersState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

object DogcatModule {

    internal val dogcatModule = DI.Module("dogcat") {
        bindSingleton<LogFiltersState> { DefaultLogFiltersState() }
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
