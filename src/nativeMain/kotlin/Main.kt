import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlin.text.Regex.Companion.escape

val r2 = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

val prefix = "\\033[31;1;4m"
val postfix = "\\033[0m"

fun main() {
    val greenColor = "\u001b[31;1;4m"
    val reset = "\u001b[0m" // to reset color to the default
    val name = greenColor + "Alex" + reset // Add green only to Alex
    println(name)

    print("$prefix[Hello]$postfix")
    println("Hello, Kotlin/Native!")

    val child = Command("adb")
        .args("logcat", "-v", "brief")
        .stdout(Stdio.Pipe)
        .spawn()

    val stdoutReader: com.kgit2.io.Reader? = child.getChildStdout()

    while (true) {
        val line = stdoutReader!!.readLine() ?: break

        if (line.contains("111", true)) {
            println("$prefix aaaaa$postfix")
        }

        val m = r2.matchEntire(line)
        if (m != null) {
         //   println("11111 $line")
            val (level, tag, owner, message) = m.destructured

            //println(line)
        }

        //println("11111 no match, $line")
    }
    //while (val line = stdoutReader!!.readLine()!!)

/*    val lines: Sequence<String> = stdoutReader?.lines()!!
    lines.forEach {
        println("11111 $it")
    }*/

/*    Command("adb")
        .arg("logcat")
        //.stdout(Stdio.Pipe)
        .spawn()
        .wait()*/

    /*Command("ping")
        .arg("-c")
        .args("5", "localhost")
        .spawn()
        .wait()*/
}
