/*
 * SPDX-FileCopyrightText: Copyright (C) 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.logger

interface CanLog {
    fun d(line: String)

    fun close()
}

class NoOpLogger : CanLog {
    override fun d(line: String) {}

    override fun close() {}
}
