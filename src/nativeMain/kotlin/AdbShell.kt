import AppConfig.COMMAND_TIMEOUT_MILLIS
import com.kgit2.kommand.exception.ErrorType
import com.kgit2.kommand.exception.KommandException
import com.kgit2.kommand.process.Child
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio.Pipe
import dogcat.DogcatException
import dogcat.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import logger.Logger
import logger.context
import kotlin.coroutines.coroutineContext

class AdbShell(
    private val dispatcherIo: CoroutineDispatcher
) : Shell {

    private lateinit var adbDevice: String

    private lateinit var adbDeviceName: String

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun lines(minLogLevel: String, userId: String): Flow<String> {
        return flow {
            Logger.d("${context()} Starting 'adb logcat'")

            val logcat = try {
                Command("adb")
                    .args(
                        listOf("-s", adbDevice, "logcat", "-v", "brief", userId, minLogLevel)
                    )
                    .stdout(Pipe)
                    .spawn()

            } catch (e: KommandException) {
                emit("--- ADB couldn't start: ${e.message}, ${e.errorType}, ${e.cause}")
                Logger.d(">>>>>>>>>>>>>>>>>>>>>>>>>>  KommandException" + e.message + e.errorType)

                return@flow
            }
            //now handle stderror?

            try {
                val stdoutReader = logcat.bufferedStdout()
                    ?: throw KommandException("Must have stdout", ErrorType.IO)

                coroutineScope {
                    val lines = produce(dispatcherIo) {
                        var numLines = 0

                        while (isActive /*&& numLines < DogcatConfig.MAX_LOG_LINES*/) {
                            val line = stdoutReader.readLine()

                            if (line != null) {
                                send(line)
                                numLines++
                            } else {
                                Logger.d(">>>>>>>>>>>>>>>>> stop break $isActive")
                                //close()
                                break
                            }
                        }
                        close()
                        Logger.d(">>>>>>>>>>>>>>>>> stop launch")
                    }

                    try {
                        lines.consumeEach {
                            emit(it)
                        }
                    } catch (e: CancellationException) {
                        Logger.d(">>>>>>>>>>>>>>>>> inner catch $e  $this")
                        logcat.shutdownSafely()
                        cancel()

                        //? However, it's important to note that CancellationException should not be caught and handled in your code. It's meant to be handled by the coroutine library. If you want to perform some action when a coroutine is cancelled, you should do it in a finally block inside the coroutine, not by catching CancellationException.

                        //? "Cancellation is implemented by throwing CancellationException, which is a RuntimeException. This design has a number of implications that are explained in this section. The most important rule of structured concurrency is that coroutine should not propagate CancellationException and should not handle it (via try/catch), unless it is doing some cleanup."

                    } catch (e: ClosedReceiveChannelException) {
                        Logger.d(">>>>>>>>>>>>>>>>> catch that no more receiving $e  $this")
                    }
                }
            } catch (e: KommandException) {
                emit("--- Exception when interacting with ADB: ${e.message}, ${e.errorType}, ${e.cause}")
                Logger.d(">>>>>>>>>>>>>>>>> outer   Kommand!  $e $this")

            } catch (e: CancellationException) {
                //might be coming from linesChannel.send(line)
                Logger.d(">>>>>>>>>>>>>>>>> outer   Cancellation!  $e $this")

            } finally {
                Logger.d(">>>>>>>>>>>>>>>>> outer   finally -- shutdown logcat!  $this")
                logcat.shutdownSafely()
            }
            Logger.d(">>>>>>>>>>>>>>>>>>>>>>>> ${context()} !!!!!!!!! Exiting flow builder ${currentCoroutineContext().isActive}")
        }
            //retry?
            .onCompletion { cause ->
                Logger.d("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! $cause\n")

                if (cause == null) {
                    Logger.d("${context()} (4) COMPLETED, loglinessource.lines $cause\n")
                    emit("--- ADB has terminated, no longer waiting for input") //will suspend
                    Logger.d("${context()} (4) COMPLETED emitted, loglinessource.lines $cause\n")
                } else {
                    Logger.d("${context()} EXIT COMPLETE $cause\r")
                }
            }
            //Do we expect more exceptions not caught from try-catch-finally above? Which can those be?
            //how to better handle?
            .catch { cause ->
                Logger.d("|||||||||||||||||||||||||||||||||||||||||||||||||||  Flow was cancelled, cleaning up resources...")
            }
            .flowOn(dispatcherIo)
    }

    override fun heartbeat(): Flow<Boolean> = flow {
        repeat(Int.MAX_VALUE) {
            val name = Command("adb")
                .args(
                    listOf("-s", adbDevice, "emu", "avd", "status")
                )
                .stdout(Pipe)
                .output()
                .stdout
                ?.lines()
                ?.first()

            val running = name?.contains("running") ?: false
            emit(running)

            delay(1000L)
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

        val child = callWithTimeout("123") {
            Command("adb")
                .args(
                    listOf("-s", adbDevice, "shell", "dumpsys", "activity", "activities")
                )
                .stdout(Pipe)
                .spawn()
        }
        var proc: String? = null
        val stdoutReader = child.bufferedStdout() ?: throw DogcatException("")

        while (coroutineContext.isActive) {
            val line = stdoutReader.readLine() ?: break
            val m = packageNamePattern.matchEntire(line)

            if (m != null) {
                val (packageName) = m.destructured
                Logger.d("LP $packageName\r")

                proc = packageName
                break
            }
        }
        child.shutdownSafely()

        return proc ?: throw RuntimeException("Didn't find running process")
    }

    override suspend fun currentEmulatorName(): String {
        val name = callWithTimeout("Couldn't launch ADB command") {
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

        Logger.d("${context()} !Emulator $name")
        return name ?: throw DogcatException("")
    }

    override suspend fun clearSource() {
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

    override suspend fun devices(): String {
        val devicesPattern = """List of devices attached\R(.*)""".toRegex()

        val output = callWithTimeout("123") {
            Command("adb")
                .args(
                    listOf("devices")
                )
                .output()
        }

        val deviceId = output.stdout?.let {
            val userId = it.lines().firstNotNullOfOrNull {
                val parts = it.split("\t")

                if (parts.size == 2 && parts[1] == "device") {
                    // This is a running and healthy device
                    parts[0]
                } else {
                    null
                }
            }

            userId

/*
            val match = devicesPattern.find(it)

            match?.let {
                val (userId) = it.destructured
                userId
            }
*/
        } ?: throw DogcatException("123")

        return deviceId ?: throw RuntimeException("UserId not found!")
    }

    //adb -s emulator-5554 emu avd name
    override suspend fun validateShellOrThrow() {
        val returnCode = callWithTimeout("ADB is not here") {
            val s = Command("adb")
                .args(
                    listOf("version")
                )
                .stdout(Pipe)
                .status()


            adbDevice = devices()

            adbDeviceName = currentEmulatorName()


            s
        }

        if (returnCode != 0) {
            throw DogcatException("ADB is not working: $returnCode")
        }
    }

    private suspend fun <T> callWithTimeout(errorPrefix: String, command: suspend CoroutineScope.() -> T): T {
        return try {
            withContext(dispatcherIo) {
                withTimeout(COMMAND_TIMEOUT_MILLIS) {
                    command()
                }
            }
        } catch (e : KommandException) {
            throw DogcatException(errorPrefix, e)
        } catch (e: TimeoutCancellationException) {
            throw DogcatException(errorPrefix, e)
        }
    }

    private fun Child.shutdownSafely() {
        try {
            kill()
        } catch (e: KommandException) {
            Logger.d(">>>>>>>>>>>>>>>>>>>>> Shutdownsafely ${e.message} $e")
        }
    }
}
