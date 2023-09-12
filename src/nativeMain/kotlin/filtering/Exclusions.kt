package filtering

class Exclusions {

    //use ADB capabilities for filtering

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