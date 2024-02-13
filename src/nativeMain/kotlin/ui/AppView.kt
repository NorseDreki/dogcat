package ui

import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*
import platform.posix.LC_ALL
import platform.posix.exit
import platform.posix.printf
import platform.posix.setlocale

@OptIn(ExperimentalForeignApi::class)
class AppView {

    fun start() {
        setlocale(LC_ALL, "en_US.UTF-8") // should be before initscr()
        initscr()

        //The keypad function enables the reading of function keys like arrow keys, Home, End, and so on.
        keypad(stdscr, true);
        noecho();

        //intrflush(stdscr, false)

        //nl()
        //Use the ncurses functions for output. My guess is that initscr changes terminal settings such that \n only performs a line feed, not a carriage return. –
        //melpomene

        nodelay(stdscr, true) //The nodelay option causes getch to be a non-blocking call. If no input is ready, getch returns ERR. If disabled (bf is  FALSE),  getch waits until a key is pressed
        //cbreak() //making getch() work without a buffer I.E. raw characters

        if (!has_colors()) {
            endwin()
            printf("Your terminal does not support color\n")
            exit(1)
        }

        //idlok¶

        use_default_colors()
        start_color()
        init_pair(1, COLOR_RED.toShort(), -1)
        init_pair(2, COLOR_GREEN.toShort(), -1)//COLOR_BLACK.toShort())
        init_pair(3, COLOR_YELLOW.toShort(), -1)
        //init_pair(4, COLOR_CYAN.toShort(), COLOR_BLACK.toShort())

        init_pair(11, COLOR_BLACK.toShort(), COLOR_RED.toShort())
        init_pair(12, COLOR_BLACK.toShort(), COLOR_WHITE.toShort())
        init_pair(6, COLOR_BLACK.toShort(), COLOR_YELLOW.toShort())
    }

    fun stop() {
        endwin()
    }
}
