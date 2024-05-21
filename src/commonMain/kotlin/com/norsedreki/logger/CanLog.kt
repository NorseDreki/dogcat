/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.logger

interface CanLog {

    fun d(line: String)

    fun close()
}

class NoOpLogger : CanLog {

    override fun d(line: String) {
        // Expected to do nothing
    }

    override fun close() {
        // Expected to do nothing
    }
}
