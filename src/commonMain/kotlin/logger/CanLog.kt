package logger

interface CanLog {
    fun d(line: String)

    fun close()
}
