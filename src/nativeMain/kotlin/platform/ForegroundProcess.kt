package platform

import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio

object ForegroundProcess {

    val FG_LINE = """^ +ResumedActivity: +ActivityRecord\{[^ ]* [^ ]* ([^ ^\/]*).*$""".toRegex()

    fun parsePs() {
        val out = Command("adb")
            .args("shell", "dumpsys", "activity", "activities")
            .stdout(Stdio.Pipe)
            .spawn()

        val stdoutReader: Reader? = out.getChildStdout()

        while (true) {
            //ensureActive() -- call in scope
            val line2 = stdoutReader!!.readLine() ?: break

            val m = FG_LINE.matchEntire(line2)
            var r = false

            if (m != null) {
                val (line_package) = m.destructured
                println("LP $line_package\r")
                //p = line_package


//                if (line_package.contains(p)) {
//                    println("line: $line_pid $line_package\r")
//                    pids.add(line_pid)
//                    r = true
//                }
            }
        }
        out.wait()

        println("ksljdhflkdjshfdddddd")
    }
}
