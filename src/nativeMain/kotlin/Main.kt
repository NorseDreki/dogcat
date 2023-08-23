import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import ncurses.*
import platform.posix.LC_CTYPE
import platform.posix.exit
import platform.posix.printf
import platform.posix.setlocale

val prefix = "\\033[31;1;4m"
val postfix = "\\033[0m"
val sm = "\u001b[263a]"

@OptIn(ExperimentalForeignApi::class, ExperimentalCoroutinesApi::class)
fun main(): Unit = memScoped {
    setlocale(LC_CTYPE, "")
    //setlocale(LC_ALL, "en_US.UTF-8");
    initscr();
    intrflush(stdscr, false);
    savetty();
    noecho();//disable auto-echoing
    /*You've set nodelay so getch will return immediately with ERR if there's no data ready from the terminal. That's why getch is returning -1 (ERR). You haven't set cbreak or raw to disable terminal buffering, so you're still getting that -- no data will come from the terminal until Enter is hit.

    So add a call to cbreak() at the start (just before or after the call to nodelay()) and it should work as you expect.
    Applications will also commonly need to react to keys instantly, without requiring the Enter key to be pressed; this is called cbreak mode, as opposed to the usual buffered input mode.
    */
    //cbreak();//making getch() work without a buffer I.E. raw characters

    //Terminals usually return special keys, such as the cursor keys or navigation keys such as Page Up and Home, as a multibyte escape sequence. While you could write your application to expect such sequences and process them accordingly, curses can do it for you, returning a special value such as curses.KEY_LEFT. To get curses to do the job, you’ll have to enable keypad mode.
    keypad(stdscr, true);//allows use of special keys, namely the arrow keys
    clear();    // empty the screen
    //timeout(0); // reads do not block
    //nodelay(sdtscr)
    //curs_set(0)
    //halfdelay(2)
    if(!has_colors()) {
        endwin();
        printf("Your terminal does not support color\n");
        exit(1);
    }
    use_default_colors()
    start_color();			/* Start color 			*/
    init_pair(1, COLOR_RED.toShort(), COLOR_BLACK.toShort());
    init_pair(2, COLOR_GREEN.toShort(), COLOR_BLACK.toShort());
    init_pair(3, COLOR_YELLOW.toShort(), COLOR_BLACK.toShort());
    init_pair(4, COLOR_CYAN.toShort(), COLOR_BLACK.toShort());


    //nonl() as Unit /* tell curses not to do NL->CR/NL on output */
    //cbreak() as Unit /* take input chars one at a time, no wait for \n */
    //noecho() as Unit /* don't echo input */
    //if (!single_step) nodelay(stdscr, TRUE)
    //idlok(stdscr, TRUE) /* allow use of insert/delete line */

    //init_color(COLOR_RED, 700, 0, 0);

    val sx = getmaxx(stdscr)
    val sy = getmaxy(stdscr)



    val fp = newpad(32767, sx)
    scrollok(fp, true)
    //keypad(fp, true)

    runBlocking {
        var a = 25;

        val logcat = Logcat()
        logcat
            .sss
            .withIndex()
            .onEach {
                //println("${it.index} ${it.value} \r\n")

                waddstr(fp, "${it.index} ")
                waddstr(fp, "${it.value.tag} ||")
                /*wattron(fp, COLOR_PAIR(1));
                waddstr(fp, "${it.value.tag} ||")
                //wprintw(fp, it.value)
                wattroff(fp, COLOR_PAIR(1))*/

                //wattron(fp, COLOR_PAIR(2));
                waddstr(fp, "${it.value.owner} ||")
                //wattroff(fp, COLOR_PAIR(2));

                when (it.value.level) {
                    "W" -> {
                        wattron(fp, COLOR_PAIR(3));
                        waddstr(fp, "${it.value.level} ||")
                        waddstr(fp, "${it.value.message} ||\n")
                        wattroff(fp, COLOR_PAIR(3));
                    }
                    "E" -> {
                        wattron(fp, COLOR_PAIR(1));
                        waddstr(fp, "${it.value.level} ||")
                        waddstr(fp, "${it.value.message} ||\n")
                        wattroff(fp, COLOR_PAIR(1));
                    }
                    "I" -> {
                        wattron(fp, COLOR_PAIR(4));
                        waddstr(fp, "${it.value.level} ||")
                        waddstr(fp, "${it.value.message} ||\n")
                        wattroff(fp, COLOR_PAIR(4));
                    }
                    else -> {
                        waddstr(fp, "${it.value.level} ||")
                        waddstr(fp, "${it.value.message} ||\n")
                    }
                }
                //waddstr(fp, "${it.value.message} \n")

                prefresh(fp, it.index, 0, 3, 0, sy - 1, sx)
                a = it.index

                yield()
            }
            .launchIn(this)

        launch(Dispatchers.Default) {
            logcat.processCommand(StartupAs.All)

            while (true) {
                var key = wgetch(stdscr);

                when (key) {
                    'f'.code -> {
                        //wprintw(stdscr, name)
                        //println(name)
                        mvwprintw(stdscr, 0, 0, "$sx:$sy")
                        //mvwprintw(stdscr, 0, 0, name)


                        clrtoeol()
                        echo()

                        val bytePtr = allocArray<ByteVar>(200)
                        getnstr(bytePtr, 200)
                        noecho()
                        wclear(fp)

                        logcat.processCommand(Filter.ByString(bytePtr.toKString()))
                        yield()
                    }

                    'q'.code -> {
                        delwin(fp); exit(0)
                    }

                    'a'.code -> {
                        a = 0
                        prefresh(fp, 0, 0, 3, 0, sy - 1, sx);
                    }

                    'z'.code -> {
                        /*a = 0
                        prefresh(fp, 0, 0, 3,0, sy-1, sx);*/
                    }

                    'w'.code -> {
                        a--
                        prefresh(fp, a, 0, 3, 0, sy - 1, sx);
                    }

                    's'.code -> {
                        a++
                        prefresh(fp, a, 0, 3, 0, sy - 1, sx);
                    }

                    'd'.code -> {
                        a += sy - 1 - 3
                        prefresh(fp, a, 0, 3, 0, sy - 1, sx);
                    }

                    'e'.code -> {
                        a -= sy - 1 - 3
                        prefresh(fp, a, 0, 3, 0, sy - 1, sx);
                    }
                    '6'.code -> {
                        logcat.processCommand(Filter.ByLogLevel("V"))
                    }
                    '7'.code -> {
                        logcat.processCommand(Filter.ByLogLevel("D"))
                    }
                    '8'.code -> {
                        logcat.processCommand(Filter.ByLogLevel("I"))
                    }
                    '9'.code -> {
                        logcat.processCommand(Filter.ByLogLevel("W"))
                    }
                    '0'.code -> {
                        logcat.processCommand(Filter.ByLogLevel("E"))
                    }
                }
            }
        }
    }
}
