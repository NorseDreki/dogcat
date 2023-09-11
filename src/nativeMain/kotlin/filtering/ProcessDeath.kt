package filtering

class ProcessDeath {

    //TODO
    val PID_KILL = """^.*Killing (\d+):([a-zA-Z0-9._:]+)/[^:]+: (.*)$""".toRegex() //.* to parse entire line, otherwise message only
    val PID_LEAVE = """^No longer want ([a-zA-Z0-9._:]+) \(pid (\d+)\): .*$""".toRegex()
    val PID_DEATH = """^Process ([a-zA-Z0-9._:]+) \(pid (\d+)\) has died.?$""".toRegex()

    fun parseProcessDeath(line: String): Pair<String, String>? {
        val kill = PID_KILL.matchEntire(line)
        //println("zzzzzz $line")

        if (kill != null) {
            val (line_pid, line_package) = kill.destructured
            return line_package to line_pid
        }

        val leave = PID_LEAVE.matchEntire(line)

        if (leave != null) {
            val (line_package, line_pid)  = leave.destructured
            return line_package to line_pid
        }

        val death = PID_DEATH.matchEntire(line)
        if (death != null) {
            val (line_package, line_pid)  = death.destructured
            return line_package to line_pid
        }

        return null
    }
}
