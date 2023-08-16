@file:OptIn(ExperimentalForeignApi::class)

import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.*
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
fun main(): Unit = memScoped {
    setlocale(LC_CTYPE, "")
    //setlocale(LC_ALL, "en_US.UTF-8");
    initscr();
    savetty();
    noecho();//disable auto-echoing
    cbreak();//making getch() work without a buffer I.E. raw characters
    keypad(stdscr, true);//allows use of special keys, namely the arrow keys
    clear();    // empty the screen
    timeout(0); // reads do not block

    //nonl() as Unit /* tell curses not to do NL->CR/NL on output */
    //cbreak() as Unit /* take input chars one at a time, no wait for \n */
    //noecho() as Unit /* don't echo input */
    //if (!single_step) nodelay(stdscr, TRUE)
    //idlok(stdscr, TRUE) /* allow use of insert/delete line */

    val fp = newpad(32767, 120)
    scrollok(fp, true)
    //keypad(fp, true)

    /*(1..100).forEach {
        //wprintw(fp, "%d - line ------------------------------------------------------------------------  \n", it);
        waddstr(fp,"*** PROCESS $it *** \n")
    }

    prefresh(fp,25, 0, 10,0, 40,120);*/

    runBlocking {
        val a2 = async(Dispatchers.Default) {
            var a=25;

            while(true) {
                var key = wgetch(stdscr);
                //if(key=='w'.code) { y_offset--; }
//        if(key=='s') { y_offset++; }
                if(key=='w'.code) {  a--;  }
                if(key=='s'.code) {  a++; }
                if(key=='q'.code) {  delwin(fp); exit(0) }

                //prefresh(fp,a, 0, 10,0, 40,120);

                //mvprintw(0, 0, "Input: $key");

                wprintw(stdscr, "Key: $key")
                wrefresh(stdscr)
                clrtoeol();

                sleep(1U)
            }
        }

        async(Dispatchers.Default) {
            val child = Command("adb")
                .args("logcat", "-v", "brief")
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader: com.kgit2.io.Reader? = child.getChildStdout()

            var i = 0
            while (true) {
                val line = stdoutReader!!.readLine()

                waddstr(fp, "$i $line\n")

                prefresh(fp, i, 0, 10,0, 40,120)
                i++
            }
            /*(0..5000).forEach {
                waddstr(fp, "$it lkjhlkjhkjhlk lkhjl jlh kjhljh .......\n")
            }
            (0..5000).forEach {
             //   waddstr(fp, "$it lkjhlkjhkjhlk lkhjl jlh kjhljh .......\n")

                prefresh(fp, it, 0, 10, 0, 40, 120)
                //sleep(1U)
            }*/
        }

        /*val a1 = launch(Dispatchers.Default) {
            val child = Command("adb")
                .args("logcat", "-v", "brief")
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader: com.kgit2.io.Reader? = child.getChildStdout()

            var i = 0
            while (true) {
                val line = stdoutReader!!.readLine()

                waddstr(fp, "$i $line\n")

                prefresh(fp, i, 0, 10,0, 40,120)
                i++
            }

            *//*(0..5000).forEach {
                val line = stdoutReader!!.readLine()

                waddstr(fp, "$line\n")

                prefresh(fp, it, 0, 10,0, 40,120)
            }*//*
        }*/
        //defer {  }




        //awaitAll(a2, a1)
    }

    //sleep(1000U)

    //getstr(aaa)
    //mvwprintw(stdscr, 0, 0, "Key: $key")
    //wrefresh(stdscr)


    /*(1..100).forEach {
        sleep(1U)
        prefresh(fp,25 + it, 0, 10,0, 40,120);
    }

    sleep(1000U)
//    exit(0)
    while (true) {
        //val ch = getwchar()

        val ll = getch()

        //prefresh(fp,26, 0, 10,0, 40,120);
        wscrl(fp, -1)
    }*/

    //sleep(1000U)
}
