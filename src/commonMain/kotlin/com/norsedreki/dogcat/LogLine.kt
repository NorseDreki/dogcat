package com.norsedreki.dogcat

sealed interface LogLine

data class Brief(
    val level: LogLevel,
    val tag: String,
    val owner: String,
    val message: String
) : LogLine

data class Unparseable(val line: String) : LogLine

enum class LogLevel(val readable: String) {
    V("Verbose"),
    D("Debug"),
    I("Info"),
    W("Warning"),
    E("Error"),
    F("Fatal")
}
