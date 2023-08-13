@file:OptIn(ExperimentalForeignApi::class)

import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.text.Regex.Companion.escape
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.memScoped
import ncurses.*

val r2 = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

val prefix = "\\033[31;1;4m"
val postfix = "\\033[0m"


val sqWidth = 10
val sqHeight = 10

val board = mutableListOf<CPointer<WINDOW>?>()

fun show() {
    /*initscr()
    defer { endwin() }
    noecho()
    curs_set(0)
    halfdelay(2)
*/
    initscr()
    noecho()
    cbreak()
    refresh()

    var starty = 0
    for (i in 0..10) {
        board.add(newwin(sqHeight, sqWidth, starty, i * sqWidth))
    }
    starty = sqHeight
    for (i in 0..10) {
        board.add(newwin(sqHeight, sqWidth, starty, i * sqWidth))
    }
    starty = sqHeight * 2
    for (i in 0..10) {
        board.add(newwin(sqHeight, sqWidth, starty, i * sqWidth))
    }

    for (window in board) {
        if (window == null) {
            println("Window was null!!")
        } else {
            box(window, 0U, 0U);
            wrefresh(window);
        }
    }

    getch()
}

@OptIn(ExperimentalForeignApi::class)
fun main() = memScoped {
    //show()
    initscr();
    val new = newwin(80 - 2, 80 - 2, 1, 1);

    scrollok(new,true);

    (1..100).forEach {
        wprintw(new, "%d - lots and lots of lines flowing down the terminal\n", it);
        wrefresh(new);
    }
    getch()
    endwin();
    getch()

    initscr()
    cbreak()
    noecho()
    clear()
    mvaddstr(0, 0, "Press any key to exit!\n")
    addstr("11\n")
    addstr("11\n")
    addstr("11\n")
    addstr("11\n")
    addstr("11\n")
    addstr("12\r\n")
    (1..100).forEach {
        printw("enter the secret password: \n");
    }
    refresh()
    getch()
    endwin()

    val greenColor = "\u001b[31;1;4m"
    val reset = "\u001b[0m" // to reset color to the default
    val name = greenColor + "Alex" + reset // Add green only to Alex
    println(name)

    print("$prefix[Hello]$postfix")
    println("Hello, Kotlin/Native!\r\n")

    (1..1000).forEach {
        addstr("Line $it")
    }

    val child = Command("adb")
        .args("logcat", "-v", "brief")
        .stdout(Stdio.Pipe)
        .spawn()

    val stdoutReader: com.kgit2.io.Reader? = child.getChildStdout()

    while (true) {
        val line = stdoutReader!!.readLine() ?: break

        //if (line.contains("111", true)) {
            //println("$prefix aaaaa$postfix")
            addstr("line\r\n")
        //}

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
