package ui

import kotlinx.cinterop.ExperimentalForeignApi
import ncurses.*
import ui.ColorMap.ColorPairs.*

@OptIn(ExperimentalForeignApi::class)
object ColorMap {

    fun allocateColor(tag: String): Int {
        if (!KNOWN_TAGS.containsKey(tag)) {
            KNOWN_TAGS[tag] = LAST_USED[0]
        }
        val color = KNOWN_TAGS[tag]!!

        if (LAST_USED.contains(color)) {
            LAST_USED.remove(color)
            LAST_USED.add(color)
        }

        return color
    }


    private val LAST_USED = mutableListOf(
        /*COLOR_RED,*/
        COLOR_GREEN,
        /*COLOR_YELLOW,*/
        COLOR_BLUE,
        COLOR_MAGENTA,
        COLOR_CYAN
    )

    private val KNOWN_TAGS = mutableMapOf(
        "dalvikvm" to COLOR_WHITE,
        "Process" to COLOR_WHITE,
        "ActivityManager" to COLOR_WHITE,
        "ActivityThread" to COLOR_WHITE,
        "AndroidRuntime" to COLOR_CYAN,
        "jdwp" to COLOR_WHITE,
        "StrictMode" to COLOR_WHITE,
        "DEBUG" to COLOR_YELLOW,
    )

    val COLOR_MAP = (LAST_USED + KNOWN_TAGS.values)
        .associateWith {
            init_pair((100 + it).toShort(), it.toShort(), -1)
            (100 + it)
        }



    val COLOR_PAIRS = listOf(
        COLOR_RED.toShort() to -1,
        COLOR_GREEN.toShort() to -1,
        COLOR_YELLOW.toShort() to -1,
    )


    enum class ColorPairs(val assignedCode: Int) {
        Red(1), Green(2), Yellow(3)
    }


    val m =
        mapOf(
            Red to (COLOR_RED.toShort() to -1),
            Green to (COLOR_GREEN.toShort() to -1),
            Yellow to (COLOR_YELLOW.toShort() to -1)

        )
/*
    enum class ColorPair(val id: Int, val foreground: Short, val background: Short) {
        RED_BLACK(1, COLOR_RED.toShort(), -1),
        GREEN_BLACK(2, COLOR_GREEN.toShort(), -1),
        YELLOW_BLACK(3, COLOR_YELLOW.toShort(), -1),
        CYAN_BLACK(4, COLOR_CYAN.toShort(), COLOR_BLACK.toShort()),
        BLACK_RED(11, COLOR_BLACK.toShort(), COLOR_RED.toShort()),
        BLACK_WHITE(12, COLOR_BLACK.toShort(), COLOR_WHITE.toShort()),
        BLACK_YELLOW(6, COLOR_BLACK.toShort(), COLOR_YELLOW.toShort());

        fun init() {
            init_pair(id, foreground, background)
        }
    }

// Initialize color pairs
    ColorPair.values().forEach { it.init() }*/
}
