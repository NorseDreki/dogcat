package platform

import com.kgit2.io.Reader
import com.kgit2.process.Command
import com.kgit2.process.Stdio

class RunningProcesses {

    val PID_LINE = """^\w+\s+(\w+)\s+\w+\s+\w+\s+\w+\s+\w+\s+\w+\s+\w\s([\w|\.|\/]+)$""".toRegex()


    private fun parseFg() {
        val out = Command("adb")
            .args("shell", "ps")
            .stdout(Stdio.Pipe)
            .spawn()

        val stdoutReader: Reader? = out.getChildStdout()

        while (true) {
            //ensureActive() -- call in scope
            val line2 = stdoutReader!!.readLine() ?: break

            val m = PID_LINE.matchEntire(line2)
            var r = false

            if (m != null) {
                val (line_pid, line_package) = m.destructured
                /*if (line_package.contains(p)) {
                    println("line: $line_pid $line_package\r")
                    pids.add(line_pid)
                    r = true
                }*/
            }
        }
    }
}