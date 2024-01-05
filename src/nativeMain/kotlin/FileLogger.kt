import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
class FileLogger : L {

    private val f: CPointer<FILE> = fopen("log.txt", "w") ?: throw RuntimeException()

    override fun d(line: String) {
        fprintf(f, "$line\r\n")
        fflush(f)
    }

    override fun close() {
        d("close logger")
        //remove()
        fflush(f)
        fclose(f)
    }
}
