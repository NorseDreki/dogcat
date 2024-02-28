package com.norsedreki.dogcat

object DogcatConfig {

    // Lower the number, less memory consumption. Also note that NCurses pad, which is the current implementation
    // to hold and scroll log, can handle 32767 lines at max.
    const val MAX_LOG_LINES = 10000

    val DEFAULT_MIN_LOG_LEVEL = LogLevel.V
}