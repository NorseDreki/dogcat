package platform

import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio

object ForegroundProcess {

    val FG_LINE = """^ +ResumedActivity: +ActivityRecord\{[^ ]* [^ ]* ([^ ^\/]*).*$""".toRegex()

    fun parsePs(): String {
        val out = Command("adb")
            .args("shell", "dumpsys", "activity", "activities")
            .stdout(Stdio.Pipe)
            .spawn()

        val stdoutReader: Reader? = out.getChildStdout()

        lateinit var proc: String

        while (true) {
            //ensureActive() -- call in scope
            val line2 = stdoutReader!!.readLine() ?: break

            val m = FG_LINE.matchEntire(line2)

            if (m != null) {
                val (line_package) = m.destructured
                println("LP $line_package\r")

                proc =  line_package
                break
            }
        }
        out.wait()

        return proc
    }
}
