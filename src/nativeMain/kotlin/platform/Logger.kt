package platform

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.removefile
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
object Logger {

    private val f: CPointer<FILE> = fopen("log.txt", "w") ?: throw RuntimeException()


    fun d(line: String) {
        fprintf(f, "$line\r\n")
        fflush(f)
    }

    fun close() {
        d("close logger")
        //remove()
        fflush(f)
        fclose(f)
    }
}
