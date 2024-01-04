package ui

import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
class DogcatPresenter {

    fun start() {


        setlocale(LC_ALL, "en_US.UTF-8") // should be before initscr()
        initscr()

        //raw();
        //keypad(stdscr, TRUE);
        //noecho();


        //intrflush(stdscr, false)
        //savetty()
        //noecho()
        //savetty()

        //nl()
        //Use the ncurses functions for output. My guess is that initscr changes terminal settings such that \n only performs a line feed, not a carriage return. –
        //melpomene

        //nodelay(stdscr, true) //The nodelay option causes getch to be a non-blocking call. If no input is ready, getch returns ERR. If disabled (bf is  FALSE),  getch waits until a key is pressed
        //cbreak(); //Disable line buffering
        //cbreak() //making getch() work without a buffer I.E. raw characters
        //keypad(stdscr, true) //allows use of special keys, namely the arrow keys
        //clear()

        //whline(topBar, '_',80); //draw line for bottom

        if (!has_colors()) {
            endwin()
            printf("Your terminal does not support color\n")
            exit(1)
        }
        use_default_colors()
        start_color()
        init_pair(1, COLOR_RED.toShort(), -1)
        init_pair(2, COLOR_GREEN.toShort(), COLOR_BLACK.toShort())
        init_pair(3, COLOR_YELLOW.toShort(), -1)
        init_pair(4, COLOR_CYAN.toShort(), COLOR_BLACK.toShort())

        init_pair(11, COLOR_BLACK.toShort(), COLOR_RED.toShort())
        init_pair(12, COLOR_BLACK.toShort(), COLOR_WHITE.toShort())
        init_pair(6, COLOR_BLACK.toShort(), COLOR_YELLOW.toShort())


        /*raw();
        noraw()*/

        //fflush(stdout); // added
        //hideCursor()
    }

    fun end() {
        endwin()
    }

    private fun hideCursor() {
        curs_set(0)
    }
}
