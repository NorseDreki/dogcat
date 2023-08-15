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
    keypad(stdscr, true);//allows use of special keys, namely the arrow keys
    clear();    // empty the screen
    timeout(0); // reads do not block

    val fp = newpad(32767, 120)
    scrollok(fp, true)
    //keypad(fp, true)

    (1..100).forEach {
        //wprintw(fp, "%d - line ------------------------------------------------------------------------  \n", it);
        waddstr(fp,"*** PROCESS $it *** \n")
    }

    prefresh(fp,25, 0, 10,0, 40,120);

    var a=25;
    var b=0;
    var c=0;
    var d=40;
    var e=120
    var f=12;

    while(true) {
        var key = wgetch(fp);
        //if(key=='w'.code) { y_offset--; }
//        if(key=='s') { y_offset++; }
        if(key=='w'.code) {  a--;  }
        if(key=='s'.code) {  a++; }
        if(key=='q'.code) {  exit(0) }

        prefresh(fp,a, 0, 10,0, 40,120);
    }

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
