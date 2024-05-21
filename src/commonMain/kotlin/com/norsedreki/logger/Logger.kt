/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.logger

object Logger : CanLog {

    private lateinit var canLog: CanLog

    fun set(l: CanLog) {
        canLog = l
    }

    override fun d(line: String) {
        canLog.d(line)
    }

    override fun close() {
        canLog.close()
    }
}
