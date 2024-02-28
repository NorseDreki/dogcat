package com.norsedreki.logger

interface CanLog {
    fun d(line: String)
    fun close()
}

class NoOpLogger : CanLog {
    override fun d(line: String) { }

    override fun close() { }
}
