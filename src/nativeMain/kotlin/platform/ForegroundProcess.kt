package platform

import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.*

object ForegroundProcess {

    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO

    val FG_LINE = """^ +ResumedActivity: +ActivityRecord\{[^ ]* [^ ]* ([^ ^\/]*).*$""".toRegex()

    suspend fun parsePackageName() = withContext(dispatcherIo) {
        val out = Command("adb")
            .args("shell", "dumpsys", "activity", "activities")
            .stdout(Stdio.Pipe)
            .spawn()

        val stdoutReader = out.getChildStdout()!!

        var proc: String? = null

        while (currentCoroutineContext().isActive) {
            val line = stdoutReader.readLine() ?: break
            val m = FG_LINE.matchEntire(line)

            if (m != null) {
                val (line_package) = m.destructured
                Logger.d("LP $line_package\r")

                proc = line_package
                break
            }
        }
        out.kill()

        proc ?: throw RuntimeException("Didn't find running process")
    }
}
