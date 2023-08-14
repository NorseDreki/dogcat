@file:OptIn(ExperimentalForeignApi::class)

import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import ncurses.*
import platform.posix.*

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
    setlocale(LC_CTYPE, "")
    //setlocale(LC_ALL, "en_US.UTF-8");
    initscr();
    savetty();
    noecho();//disable auto-echoing
    cbreak();//making getch() work without a buffer I.E. raw characters
    //keypad(stdscr, true);//allows use of special keys, namely the arrow keys
    //clear();    // empty the screen
    timeout(0); // reads do not block

    val fp = newpad(50, 120)
    scrollok(fp, true)

    (1..200).forEach {
        //wprintw(fp, "%d - line ------------------------------------------------------------------------  \n", it);
        waddstr(fp,"*** PROCESS 01 ***")
    }

    prefresh(fp,0, 0, 0,0, 20,120);

    sleep(1000U)

    getch()
    //exit(0)
    while (true) {
        val ch = getwchar()

        wscrl(fp, -1)
    }

    //val p = newpad(80,COLS);
    //keypad(p, true);

    //getmaxyx(stdscr, height, width);
    val p = newwin(50 - 2, 150 - 2, 1, 1);

    scrollok(p,true);

   /* val child1 = Command("adb")
        .args("logcat", "-v", "brief")
        .stdout(Stdio.Pipe)
        .spawn()*/

    //val stdoutReader1: com.kgit2.io.Reader? = child1.getChildStdout()

    (1..100).forEach {
        //waddstr(p, "aaa$it")
        val sm = "\u001b[263a]"
        try {
            //val line = stdoutReader1!!.readLine()!!// ?: exit(127)
            //sleep(1U)
            wprintw(p, "\u2588")
            wprintw(p, "\u263a")
            wprintw(p, "%d - $sm line  \n", it);
        } catch (e: Exception) {
            wprintw(p, "----------------------------> ${e.message}")

            wprintw(p, "----------------------------> ${e.cause}")
            sleep(100U)
        }
        wrefresh(p)
    }


    //prefresh(p,0, 0, 0,0, LINES-1,COLS-1);
    //prefresh(pad, mypadpos, 0, 0, 0, mrow, mcol);


    while (true) {
        val ch = getwchar()

        //wget_wch()
        val c = Char(ch)

        wscrl(p, -1)

        when (ch) {
            KEY_UP -> wprintw(p,"Up\n")
            KEY_LEFT -> wprintw(p,"Left\n")
            KEY_RIGHT -> wprintw(p,"Right\n")
            KEY_BACKSPACE -> wprintw(p,"Backspace\n")
            KEY_ENTER -> {
                wprintw(p,"You pressed Enter\n")
                //printw("%u\n", ch)
            }

            else ->{}// printw("%u\n", ch)
        }

        //wprintw(p, "$KEY_UP $ch $c\n")
        //wprintw(p, "Auftrag \tName \t\t\tZeit\n");
        wrefresh(p)
        //prefresh(p, 0, 0, 0, 0, LINES - 1, COLS -1)
    }

    //getmaxyx(stdscr, mrow, mcol);
    getch()
    //endwin();
    //exit(0)

    //getmaxx()
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
