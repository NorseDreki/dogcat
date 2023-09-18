package dogcat

import Config.tagWidth

sealed interface LogLine

data class Parsed(
    val level: String,
    val tag: String,
    val owner: String,
    val message: String
) : LogLine {
    // or maybe create a dedicated Tag class

}

//do we really need unparsed lines? how many of them?
data class Original(val line: String) : LogLine //maybe to have inline
