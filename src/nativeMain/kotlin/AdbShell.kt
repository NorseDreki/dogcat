import AppConfig.COMMAND_TIMEOUT_MILLIS
import AppConfig.DEVICE_POLLING_PERIOD_MILLIS
import com.kgit2.kommand.exception.KommandException
import com.kgit2.kommand.process.Child
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio.Pipe
import com.norsedreki.dogcat.DogcatException
import com.norsedreki.dogcat.Shell
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlin.coroutines.coroutineContext

class AdbShell(
    private val dispatcherIo: CoroutineDispatcher
) : Shell {

    private lateinit var adbDevice: String

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun logLines(minLogLevel: String, appId: String): Flow<String> {
        return flow {
            Logger.d("${context()} Starting ADB Logcat")

            val logcat = callWithTimeout("Could not start ADB Logcat") {
                Command("adb")
                    .args(
                        listOf("-s", adbDevice, "logcat", "-v", "brief", appId, minLogLevel)
                    )
                    .stdout(Pipe)
                    .stderr(Pipe)
                    .spawn()
            }
            //now handle stderror?

            val stdoutReader = logcat.bufferedStdout()
                ?: throw DogcatException("Error in a dependent library, could not get STDOUT of ADB Logcat")

            coroutineScope {
                val lines = produce(dispatcherIo) {
                    while (isActive) {
                        val line = stdoutReader.readLine()

                        if (line != null) {
                            send(line)
                        } else {
                            break
                        }
                    }
                    close()
                }

                try {
                    lines.consumeEach {
                        emit(it)
                    }
                } catch (e: ClosedReceiveChannelException) {
                    Logger.d("Could not consume all elements in 'lines' channel: $e ")

                } finally {
                    Logger.d("COMPLETION (0): Cleaning up resources after consuming log lines")

                    logcat.shutdownSafely()
                    cancel()
                }
            }
        }
            .onCompletion { cause ->
                Logger.d("${context()} COMPLETION (1): ADB logcat has terminated, maybe with exception: $cause")
            }
            .flowOn(dispatcherIo)
    }

    override fun isDeviceOnline(): Flow<Boolean> = flow {
        Logger.d("!!!!!!!! NEW DEVICE RUNNING")

        repeat(Int.MAX_VALUE) {

            val name = callWithTimeout("123") {
                Command("adb")
                    .args(
                        listOf("-s", adbDevice, "emu", "avd", "status")
                    )
                    .stdout(Pipe)
                    .output()
                    .stdout
                    ?.lines()
                    ?.first()
            }

            //maybe catch and ignore exceptions, or ignore some part

            /*➜  tools adb -s emulator-5554 emu avd name
                error: could not connect to TCP port 5554: Connection refused
            ➜  tools adb -s emulator-5555 emu avd name
                error: could not connect to TCP port 5555: Connection refused
            ➜  tools adb -s emulator-555512 emu avd name
                error: could not connect to TCP port 555512: Connection refused*/

            val running = name?.contains("running") ?: false

            /*if (!running) {
                adbDevice = ""
            }*/

            Logger.d("RUNNING? $running")

            emit(running)

            delay(DEVICE_POLLING_PERIOD_MILLIS)
        }
    }
        .flowOn(dispatcherIo)

    override suspend fun appIdFor(packageName: String): String {
        val appIdContext =
            """Packages:\R\s+Package\s+\[$packageName]\s+\(.*\):\R\s+(?:appId|userId)=(\d*)""".toRegex()

        val output = callWithTimeout("123") {
            Command("adb")
                .args(
                    listOf("-s", adbDevice, "shell", "dumpsys", "package")
                )
                .arg(packageName)
                .stdout(Pipe)
                .output()
        }

        val appId = output.stdout?.let {
            val match = appIdContext.find(it)

            match?.let {
                val (id) = it.destructured
                id
            }
        }

        return appId
            ?: throw DogcatException("App ID is not found for the package '$packageName'. Package is not installed on device.")
    }

    override suspend fun foregroundPackageName(): String {
        val packageNamePattern = """^ +ResumedActivity: +ActivityRecord\{[^ ]* [^ ]* ([^ ^/]*).*$""".toRegex()

        val child = callWithTimeout("Could not get a dump of running activities") {
            Command("adb")
                .args(
                    listOf("-s", adbDevice, "shell", "dumpsys", "activity", "activities")
                )
                .stdout(Pipe)
                .spawn()
        }
        var packageName: String? = null
        val stdoutReader = child.bufferedStdout() ?: throw DogcatException("!!!!!!!!")

        while (coroutineContext.isActive) {
            val line = stdoutReader.readLine() ?: break
            val match = packageNamePattern.matchEntire(line)

            if (match != null) {
                val (pn) = match.destructured

                packageName = pn
                break
            }
        }
        child.shutdownSafely()

        return packageName ?: throw DogcatException("Didn't find running process")
    }

    override suspend fun deviceName(): String {
        val name = callWithTimeout("Couldn't get label for $adbDevice") {
            Command("adb")
                .args(
                    listOf("-s", adbDevice, "emu", "avd", "name")
                )
                .stdout(Pipe)
                .output()
                .stdout
                ?.lines()
                ?.first()
        }

        Logger.d("deviceName $name $adbDevice")

        val result =
            if (!name.isNullOrEmpty()) name
            else adbDevice

        return result
    }

    override suspend fun clearLogLines() {
        val exitCode = callWithTimeout("Could not clear logcat") {
            Command("adb")
                .args(
                    listOf("-s", adbDevice, "logcat", "-c")
                )
                .status()
        }

        if (exitCode != 0) {
            throw DogcatException("Could not clear logcat, exit code: $exitCode")
        }

        Logger.d("${context()} Exit code for 'adb logcat -c': ${exitCode}")
    }

    override suspend fun firstRunningDevice(): String {
        val output = callWithTimeout("Could not get first running device") {
            Command("adb")
                .args(
                    listOf("devices")
                )
                .output()
        }

        val device = output.stdout?.let {
            it.lines()
                .firstNotNullOfOrNull {
                    val parts = it.split("\t")

                    if (parts.size == 2 && parts[1] == "device") {
                        // This is a running and healthy device
                        parts[0]
                    } else {
                        null
                    }
                }
        } ?: throw DogcatException("ADB returned no running devices")

        return device
    }

    override suspend fun validateShellOrThrow() {
        val m = "Android Debug Bridge (ADB), a part of Android"
        val returnStatus = callWithTimeout(m) {
            val status = Command("adb")
                .args(
                    listOf("version")
                )
                .stdout(Pipe)
                .status()

            //maybe just invoking this would be enough for ADB test
            adbDevice = firstRunningDevice()

            status
        }

        if (returnStatus != 0) {
            throw DogcatException("Android Debug Bridge (ADB) is found but returned an error code $returnStatus.")
        }
    }

    private suspend fun <T> callWithTimeout(errorPrefix: String, command: suspend CoroutineScope.() -> T): T {
        val m = "Android Debug Bridge (ADB), a part of Android"

        return try {
            withContext(dispatcherIo) {
                withTimeout(COMMAND_TIMEOUT_MILLIS) {
                    command()
                }
            }
        } catch (e: KommandException) {
            throw DogcatException(errorPrefix, e)
        } catch (e: TimeoutCancellationException) {
            throw DogcatException(errorPrefix, e)
        }
    }

    private fun Child.shutdownSafely() {
        try {
            kill()
        } catch (e: KommandException) {
            Logger.d("Kommand library has thrown an exception when trying to kill a process: ${e.message} $e")
        }
    }
}
