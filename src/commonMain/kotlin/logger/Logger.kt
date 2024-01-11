package logger

object Logger : CanLog {

    private var logger: CanLog? = null

    fun set(l: CanLog) {
        logger = l
    }

    override fun d(line: String) {
        logger?.d(line)
    }

    override fun close() {
        logger?.close()
    }
}
