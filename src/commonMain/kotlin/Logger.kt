import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
var logger: L? = null

object Logger {

    fun set(l: L) = run { logger = l }

    fun d(line: String) = logger?.d(line)

    fun close() = logger?.close()
}
