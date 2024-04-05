/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import dogcat.LogLinesSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class FakeLogSource : LogLinesSource {

    private val source = MutableSharedFlow<String>()
    private val source1 = MutableStateFlow("")

    suspend fun emitLine(line: String) {
        source.emit(line)
    }

    override fun lines() = flow {
        println("emitting lines")
        emit("1")
        println("em 1")
        delay(20)
        emit("2")
        println("em 2")
        delay(20)
        throw RuntimeException("Boom")
    }

    suspend override fun clear(): Boolean {
        TODO("Not yet implemented")
    }
}
