/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

sealed interface LogLine

data class Brief(
    val level: LogLevel,
    val tag: String,
    val owner: String,
    val message: String,
) : LogLine

data class Unparseable(val line: String) : LogLine

enum class LogLevel(val label: String) {
    V("Verbose"),
    D("Debug"),
    I("Info"),
    W("Warning"),
    E("Error"),
    F("Fatal")
}
