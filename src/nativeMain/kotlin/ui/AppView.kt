package ui

import dogcat.DogcatException
import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*
import platform.posix.LC_ALL
import platform.posix.exit
import platform.posix.printf
import platform.posix.setlocale

@OptIn(ExperimentalForeignApi::class)
class AppView : HasLifecycle {

    override suspend fun start() {
        setlocale(LC_ALL, "en_US.UTF-8")
        initscr()

        keypad(stdscr, true);
        noecho();

        // The nodelay option causes getch to be a non-blocking call. If no input is ready, getch returns ERR.
        // If disabled (bf is  FALSE),  getch waits until a key is pressed
        nodelay(stdscr, true)
        //cbreak or raw, to make wgetch read unbuffered data, i.e., not waiting for '\n'.

        if (!has_colors()) {
            endwin()

            throw DogcatException("Your terminal does not support color")
            //printf("Your terminal does not support color\n")
            exit(1)
        }

        use_default_colors()
        start_color()

        CommonColors.entries.forEach {
            init_pair(it.colorPairCode.toShort(), it.foregroundColor, it.backgroundColor)
        }
    }

    override suspend fun stop() {
        endwin()
    }
}
