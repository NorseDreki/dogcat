/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

import com.norsedreki.dogcat.DogcatConfig.MAX_LOG_LINES
import com.norsedreki.dogcat.LogFilter.ByPackage
import com.norsedreki.dogcat.LogFilter.MinLogLevel
import com.norsedreki.dogcat.LogFilter.Substring
import com.norsedreki.dogcat.state.LogFiltersState
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.withIndex

@OptIn(ExperimentalCoroutinesApi::class)
class LogLines(
    private val lineParser: LogLineParser,
    private val filtersState: LogFiltersState,
    private val shell: Shell,
    private val dispatcherCpu: CoroutineDispatcher
) {
    private lateinit var scope: CoroutineScope
    private lateinit var sharedLines: Flow<String>

    suspend fun capture(restartSource: Boolean = true): Flow<IndexedValue<LogLine>> {
        if (restartSource) {
            if (this::scope.isInitialized) {
                Logger.d("${context()} Cancel coroutine scope for log lines collection")

                scope.cancel()
            }

            // We need a dedicated job otherwise 'scope.cancel()' would cancel parent scope as well
            scope = CoroutineScope(coroutineContext + dispatcherCpu + Job())
            sharedLines = createSharedLines()

            Logger.d("${context()} Created shared log lines by re-starting ADB logcat")
        }

        return filtersState.state
            .flatMapConcat { it.values.asFlow() }
            .filterIsInstance<Substring>()
            .flatMapLatest { filter ->
                sharedLines.filter { it.contains(filter.substring, ignoreCase = true) }
            }
            .map { lineParser.parse(it) }
            .bufferedTransform(
                shouldEmptyBuffer(),
                transformItem(),
            )
            .withIndex()
            .onCompletion { Logger.d("${context()} COMPLETION (3): Full log lines pipeline") }
            .flowOn(dispatcherCpu)
    }

    fun stop() {
        if (this::scope.isInitialized) {
            scope.cancel()
        }
    }

    private fun transformItem() = { buffer: List<LogLine>, item: LogLine ->
        when (item) {
            is Brief -> {
                if (buffer.isEmpty()) {
                    item
                } else {
                    // Do not display tag if it's the same as in previous item
                    Brief(item.level, "", item.owner, item.message)
                }
            }
            is Unparseable -> item
        }
    }

    private fun shouldEmptyBuffer() = { buffer: List<LogLine>, item: LogLine ->
        when (item) {
            is Brief -> {
                when {
                    buffer.isNotEmpty() -> {
                        // All items in buffer have same tags, otherwise buffer is drained
                        val anyPreviousItem = buffer[0]

                        val doesTagOfPreviousItemMatchCurrent =
                            (anyPreviousItem as? Brief)?.tag?.contains(item.tag) ?: false

                        when {
                            // Don't drain buffer since each item has the same tag
                            doesTagOfPreviousItemMatchCurrent -> false

                            // Drain buffer since there is a new tag
                            else -> true
                        }
                    }
                    else -> false
                }
            }

            // We don't group 'Unparseable' since it doesn't have a tag
            is Unparseable -> false
        }
    }

    private fun createSharedLines(): Flow<String> {
        val filters = filtersState.state.value

        val minLogLevel =
            filters[MinLogLevel::class]?.let { "*:${(it as MinLogLevel).logLevel}" } ?: ""

        val appId = filters[ByPackage::class]?.let { "--uid=${(it as ByPackage).appId}" } ?: ""

        return shell
            .logLines(minLogLevel, appId)
            .shareIn(
                scope,
                SharingStarted.Lazily,
                MAX_LOG_LINES,
            )
            .onCompletion {
                Logger.d("${context()} COMPLETION (2): ADB logcat log lines shared by 'shareIn'")
            }
    }
}
