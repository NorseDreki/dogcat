object AppConfig {

    const val INPUT_KEY_DELAY_MILLIS = 30L

    const val DEFAULT_TAG_WIDTH = 23

    const val MAX_TAG_WIDTH = 50

    const val APP_LOG_FILENAME = "debug.log"

    const val COMMAND_TIMEOUT_MILLIS = 3000L

    const val DEVICE_POLLING_PERIOD_MILLIS = 1000L

    const val TAG_COLOR_PAIR_OFFSET = 100

    const val LOG_LINES_VIEW_BOTTOM_MARGIN = 4

    const val STATUS_VIEW_AUTOSCROLL_LEFT_MARGIN = 15

    const val STATUS_VIEW_BOTTOM_MARGIN = 2

    const val LOG_LEVEL_WIDTH = 1 + 3 + 1 // space, level, space

    const val LOG_LINE_ESCAPE_REGEX_STRING = """[\t\n\r\\b\f\v\a\e]"""

    const val DEFAULT_LOCALE = "en_US.UTF-8"

    const val EXIT_CODE_ERROR = 1

    // Make sure pattern matching works on MinGW using '\n'
    // There is no 'line.separator' or 'Environment.NewLine'
    //"\r?\n|\r".toRegex()
    //To match with any Unicode linebreak sequence, we can use the linebreak matcher \R
    //\\R
    //[\r\n]+
    const val LINE_SEPARATOR = "\n"
    //If you write this in common module, it should get translated to correct value on respective platform. No need for expect-actual for this.
    //By the Kotlin native compiler when it builds native code for respective platform
}
