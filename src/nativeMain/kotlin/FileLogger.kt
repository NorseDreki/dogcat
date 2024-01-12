import AppConfig.APP_LOG_FILENAME
import AppConfig.LINE_SEPARATOR
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import logger.CanLog
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
class FileLogger : CanLog {

    // I/O is not cool for field initializers, but would be OK when debugging
    private val file: CPointer<FILE> = fopen(APP_LOG_FILENAME, "w")
            ?: throw RuntimeException("Was not able to open log file for writing.")

    override fun d(line: String) {
        fprintf(file, "$line$LINE_SEPARATOR")
        fflush(file)
    }

    override fun close() {
        fflush(file)
        fclose(file)
    }
}
