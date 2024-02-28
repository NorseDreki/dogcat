package com.norsedreki.dogcat.state

import com.norsedreki.dogcat.DogcatConfig.DEFAULT_MIN_LOG_LEVEL
import com.norsedreki.dogcat.LogFilter
import com.norsedreki.dogcat.LogFilter.MinLogLevel
import com.norsedreki.dogcat.LogFilter.Substring
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass


typealias LogFilters = Map<KClass<out LogFilter>, LogFilter>

interface LogFiltersState {

    val state: StateFlow<LogFilters>

    suspend fun apply(filter: LogFilter)

    suspend fun reset(filterClass: KClass<out LogFilter>)
}

class DefaultLogFiltersState : LogFiltersState {

    private val defaultFilters: LogFilters =
        mapOf(
            Substring::class to Substring(""),
            MinLogLevel::class to MinLogLevel(DEFAULT_MIN_LOG_LEVEL)
        )

    private val stateSubject = MutableStateFlow(defaultFilters)
    override val state = stateSubject.asStateFlow()

    override suspend fun apply(filter: LogFilter) {
        val next = stateSubject.value + (filter::class to filter)
        stateSubject.emit(next)
    }

    override suspend fun reset(filterClass: KClass<out LogFilter>) {
        val default = defaultFilters[filterClass]

        val next = if (default != null) {
            stateSubject.value + (filterClass to default)
        } else {
            stateSubject.value - filterClass
        }

        stateSubject.emit(next)
    }
}
