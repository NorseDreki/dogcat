object AppConfig {

    const val INPUT_KEY_DELAY_MILLIS = 30L

    const val DEFAULT_TAG_WIDTH = 23

    const val APP_LOG_FILENAME = "log.txt"

    const val COMMAND_TIMEOUT_MILLIS = 3000L

    const val INPUT_FILTER_PREFIX = " Filter: "

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
