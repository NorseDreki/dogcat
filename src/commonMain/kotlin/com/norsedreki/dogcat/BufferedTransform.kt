/*
 * SPDX-FileCopyrightText: Copyright 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> Flow<T>.bufferedTransform(
    shouldEmptyBuffer: (List<T>, T) -> Boolean,
    transformItem: (List<T>, T) -> T,
): Flow<T> =
    flow {
        val storage = mutableListOf<T>()

        collect { item ->
            val willEmpty = shouldEmptyBuffer(storage, item)
            if (willEmpty) {
                // storage.onEach { emit(it) }
                storage.clear()
            }

            val newItem = transformItem(storage, item)
            storage.add(newItem)
            emit(newItem)
        }
    }

fun <T> Flow<T>.debounceRepetitiveKeys(debounceTime: Duration): Flow<T> =
    flow {
        var lastKey: T? = null
        var lastEmissionTime = TimeSource.Monotonic.markNow()

        collect { key ->
            val currentTime = TimeSource.Monotonic.markNow()
            if (key != lastKey || currentTime.elapsedNow() - lastEmissionTime.elapsedNow() >= debounceTime) {
                emit(key)
                lastKey = key
                lastEmissionTime = currentTime
            }
        }
    }

fun <T> Flow<T>.windowed(time: Duration): Flow<List<T>> =
    flow {
        val window = mutableListOf<T>()
        val startTime = TimeSource.Monotonic.markNow()

        collect { value ->
            window.add(value)
            if (TimeSource.Monotonic.markNow() - startTime >= time) {
                emit(window.toList())
                window.clear()
            }
        }

        if (window.isNotEmpty()) {
            emit(window.toList())
        }
    }
