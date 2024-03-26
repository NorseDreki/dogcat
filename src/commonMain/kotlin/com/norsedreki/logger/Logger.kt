/*
 * SPDX-FileCopyrightText: Copyright 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.logger

object Logger : CanLog {

    private lateinit var logger: CanLog

    fun set(l: CanLog) {
        logger = l
    }

    override fun d(line: String) {
        logger.d(line)
    }

    override fun close() {
        logger.close()
    }
}
