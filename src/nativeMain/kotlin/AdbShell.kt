import AppConfig.COMMAND_TIMEOUT_MILLIS
import com.kgit2.kommand.exception.KommandException
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import dogcat.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import logger.Logger
import logger.context

class AdbShell(
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO
) : Shell {
    override fun lines(minLogLevel: String, userId: String): Flow<String> =
        flow {
            Logger.d("${context()} Starting adb logcat")

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
                    "!!!!!!!!!!! Runtime! ${e.message} ${context()}"
                )
            }

            Logger.d("${context()} !!!!!!!!! Killing logcat ${currentCoroutineContext().isActive}")
            // tried timeout, need async IO so badly
            // command would be killed when next line appears.
            // also, no leftover adb upon app exit
            logcat.kill()
        }

    override suspend fun userIdFor(packageName: String) = withContext(dispatcherIo) {
        val UID_CONTEXT = """Packages:\R\s+Package\s+\[$packageName]\s+\(.*\):\R\s+(?:appId|userId)=(\d*)""".toRegex()
        //val UID_CONTEXT1 = """Packages:\n\s+Package\s+\[$packageName]\s+\(.*\):\n\s+appId=(\d*)""".toRegex()

        val output = withTimeout(COMMAND_TIMEOUT_MILLIS) {
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
        val name = Command("adb")
            .args(
                listOf("emu", "avd", "name")
            )
            .stdout(Stdio.Pipe)
            .output()
            .stdout
            ?.lines()
            ?.first()

        Logger.d("${context()} !Emulator $name")
        name
    }

    override suspend fun foregroundPackageName() = withContext(dispatcherIo) {
        val FG_LINE = """^ +ResumedActivity: +ActivityRecord\{[^ ]* [^ ]* ([^ ^/]*).*$""".toRegex()

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

        Logger.d("${context()} Exit code for 'adb logcat -c': ${childStatus}")

        return childStatus == 0
    }

    override suspend fun devices(): List<String> = withContext(dispatcherIo) {
        val DEVICES = """List of devices attached\R(.*)""".toRegex()

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

    override fun heartbeat(): Flow<Boolean> = flow {
        repeat(Int.MAX_VALUE) {
            val name = Command("adb")
                .args(
                    listOf("emu", "avd", "status")
                )
                .stdout(Stdio.Pipe)
                .output()
                .stdout
                ?.lines()
                ?.first()

            val running = name?.contains("running") ?: false

            Logger.d("${context()} !Emulator $name")

            emit(running)

            delay(1000L)
        }
    }

    //adb -s emulator-5554 emu avd name
    override suspend fun isShellAvailable(): Boolean {
        return withTimeout(1000) {
            val s =try {
                Command("adb")
                    .args(
                        listOf("version")
                    )
                    .stdout(Stdio.Pipe)
                    .status()
            } catch (e: KommandException) {
                //whoa exception and not code if command not found
                // maybe use 'which adb' instead
                Logger.d("KommandException" + e.message + e.cause)
                2
            }
            s == 0
        }
    }
}
