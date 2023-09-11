package filtering

class Exclusions {

    companion object {
        val excludedTags = hashSetOf(
            "droid.apps.mai",
            "OpenGLRenderer",
            "CpuPowerCalculator",
            "EGL_emulation",
            "libEGL",
            "Choreographer",
            "HeterodyneSyncer"
        )
    }
}