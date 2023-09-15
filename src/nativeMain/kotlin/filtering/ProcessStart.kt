package filtering

import dogcat.LogLine
import dogcat.Parsed

class ProcessStart {

    val PID_START = """^.*: Start proc ([a-zA-Z0-9._:]+) for ([a-z]+ [^:]+): pid=(\d+) uid=(\d+) gids=(.*)$""".toRegex()
    val PID_START_5_1 = """^.*: Start proc (\d+):([a-zA-Z0-9._:]+)/[a-z0-9]+ for (.*)$""".toRegex()
    val PID_START_DALVIK = """^E/dalvikvm\(\s*(\d+)\): >>>>> ([a-zA-Z0-9._:]+) \[ userId:0 \| appId:(\d+) \]$""".toRegex()

    fun parseProcessStart(line: String): Pair<String, String>? {
        val m = PID_START_5_1.matchEntire(line)

        if (m != null) {
            val (line_pid, line_package, target) = m.destructured
            return line_package to line_pid
        }

        val m2 = PID_START.matchEntire(line)

        if (m2 != null) {
            val (line_package, target, line_pid, line_uid, line_gids) = m2.destructured
            return line_package to line_pid
        }

        val m3 = PID_START_DALVIK.matchEntire(line)
        if (m3 != null) {

            val (line_pid, line_package, line_uid) = m3.destructured
            return line_package to line_pid
        }

        //println("tried to parse $line \r")

        return null
    }

    /*private fun trackProcesses(it: String): LogLine {
        val ps = processStart.parseProcessStart(it)

        return if (ps != null) {
            if (ps.first.contains(p)) {
                pids.add(ps.second)
            }
            Parsed("E", ps.first, ps.second, ps.second)
        } else {
            val pe = processEnd.parseProcessDeath(it)
            if (pe != null) {
                if (pe.first.contains(p)) {
                    pids.remove(pe.second)
                }

                Parsed("E", pe.first, pe.second, pe.second)
            } else {
                lineParser.parse(it)
            }
        }
    }*/
}
