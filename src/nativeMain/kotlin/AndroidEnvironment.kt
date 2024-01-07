import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalStdlibApi::class)
class AndroidEnvironment(
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO
) : Environment {
    override fun lines(minLogLevel: String, userId: String): Flow<String> =
        flow {
            Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Starting adb logcat")

            val logcat = Command("adb")
                .args(
                    listOf("logcat", "-v", "brief", userId, minLogLevel)
                )
                .stdout(Stdio.Pipe)
                .spawn()

            val stdoutReader = logcat.bufferedStdout()!!// .getChildStdout()!!

            try {
                while (true) {
                    val line = stdoutReader.readLine() ?: break
                    emit(line)
                    //yield() //?
                }
            } catch (e: CancellationException) {
                Logger.d("!!!!!!!!!!! Cancellation! $e")
            } catch (e: RuntimeException) {
                Logger.d(
                    "!!!!!!!!!!! Runtime! ${e.message} [${(currentCoroutineContext()[CoroutineDispatcher])}]"
                )
            }

            Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] !!!!!!!!! Killing logcat ${currentCoroutineContext().isActive}")
            // tried timeout, need async IO so badly
            // command would be killed when next line appears.
            // also, no leftover adb upon app exit
            logcat.kill()
        }

    override suspend fun userIdFor(packageName: String) = withContext(dispatcherIo) {
        val UID_CONTEXT = """Packages:\n\s+Package\s+\[$packageName]\s+\(.*\):\n\s+userId=(\d*)""".toRegex()

        val output = withTimeout(Config.AdbCommandTimeoutMillis) {
            Command("adb")
                .args(
                    listOf("shell", "dumpsys", "package")
                )
                .arg(packageName)
                .stdout(Stdio.Pipe)
                .output()
        }

        val userId = output.stdout?.let {
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
            .args(
                listOf("emu", "avd", "name")
            )
            .stdout(Stdio.Pipe)
            .output()
            .stdout
            ?.lines()
            ?.first()
    }

    override suspend fun foregroundPackageName() = withContext(dispatcherIo) {
        val FG_LINE = """^ +ResumedActivity: +ActivityRecord\{[^ ]* [^ ]* ([^ ^\/]*).*$""".toRegex()

        val out = Command("adb")
            .args(
                listOf("shell", "dumpsys", "activity", "activities")
            )
            .stdout(Stdio.Pipe)
            .spawn()

        val stdoutReader = out.bufferedStdout()!!// getChildStdout()!!

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
                .args(
                    listOf("logcat", "-c")
                )
                .status()
        }

        Logger.d("[${(currentCoroutineContext()[CoroutineDispatcher])}] Exit code for 'adb logcat -c': ${childStatus}")

        return childStatus == 0
    }

    override suspend fun devices(): List<String> = withContext(dispatcherIo) {
        val DEVICES = """List of devices attached\n(.*)""".toRegex()

        val output = withContext(dispatcherIo) {
            Command("adb")
                .args(
                    listOf("devices")
                )
                .output()
        }

        val userId = output.stdout?.let {
            println("11111 $it")
            val match = DEVICES.find(it)
            match?.let {
                val (userId) = it.destructured
                userId
            }
        } ?: ""

        listOf(userId) ?: throw RuntimeException("UserId not found!")
    }
}
