import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlinx.coroutines.*

@OptIn(ExperimentalStdlibApi::class)
class AndroidEnvironment(
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO
) : Environment {
    override suspend fun userIdFor(packageName: String) = withContext(dispatcherIo) {
        val UID_CONTEXT = """Packages:\n\s+Package\s+\[$packageName]\s+\(.*\):\n\s+userId=(\d*)""".toRegex()

        val output = withTimeout(Config.AdbCommandTimeoutMillis) {
            Command("adb")
                .args("shell", "dumpsys", "package")
                .arg(packageName)
                .stdout(Stdio.Pipe)
                .output()
        }

        val userId = output?.let {
            val match = UID_CONTEXT.find(it)
            match?.let {
                val (userId) = it.destructured
                userId
            }
        }

        userId ?: throw RuntimeException("UserId not found!")
    }

    override suspend fun currentEmulatorName() = withContext(Dispatchers.IO) {
            Command("adb")
                .args("emu", "avd", "name")
                .stdout(Stdio.Pipe)
                .output()
                ?.lines()
                ?.first()
    }

    override suspend fun foregroundPackageName() = withContext(dispatcherIo) {
        val FG_LINE = """^ +ResumedActivity: +ActivityRecord\{[^ ]* [^ ]* ([^ ^\/]*).*$""".toRegex()

        val out = Command("adb")
            .args("shell", "dumpsys", "activity", "activities")
            .stdout(Stdio.Pipe)
            .spawn()

        val stdoutReader = out.getChildStdout()!!

        var proc: String? = null

        while (currentCoroutineContext().isActive) {
            val line = stdoutReader.readLine() ?: break
            val m = FG_LINE.matchEntire(line)

            if (m != null) {
                val (line_package) = m.destructured
                Logger.d("LP $line_package\r")

                proc = line_package
                break
            }
        }
        out.kill()

        proc ?: throw RuntimeException("Didn't find running process")
    }

    override suspend fun clearSource(): Boolean {
        val childStatus = withContext(dispatcherIo) {
                Command("adb")
                    .args("logcat", "-c")
                    .status()
        }

        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Exit code for 'adb logcat -c': ${childStatus.code}")

        return childStatus.code == 0
    }
}
