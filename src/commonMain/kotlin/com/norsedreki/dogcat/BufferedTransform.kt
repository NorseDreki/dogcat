/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> Flow<T>.bufferedTransform(
    shouldEmptyBuffer: (List<T>, T) -> Boolean,
    transformItem: (List<T>, T) -> T
): Flow<T> = flow {
    val storage = mutableListOf<T>()

    collect { item ->
        val willEmpty = shouldEmptyBuffer(storage, item)
        if (willEmpty) {
            storage.clear()
        }

        val newItem = transformItem(storage, item)
        storage.add(newItem)
        emit(newItem)
    }
}
