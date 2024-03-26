/*
 * SPDX-FileCopyrightText: Copyright 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

sealed interface LogFilter {

    data class Substring(
        val substring: String,
    ) : LogFilter

    data class MinLogLevel(
        val logLevel: LogLevel,
    ) : LogFilter

    data class ByPackage(
        val packageName: String,
        val appId: String,
    ) : LogFilter
}
