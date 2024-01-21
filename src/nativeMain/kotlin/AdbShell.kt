import AppConfig.COMMAND_TIMEOUT_MILLIS
import com.kgit2.kommand.exception.ErrorType
import com.kgit2.kommand.exception.KommandException
import com.kgit2.kommand.process.Child
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import dogcat.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import logger.Logger
import logger.context

class AdbShell(
    private val dispatcherIo: CoroutineDispatcher
) : Shell {

    private fun Child.shutdownSafely() {
        try {
            kill()
        } catch (e: KommandException) {
            Logger.d(">>>>>>>>>>>>>>>>>>>>> Shutdownsafely ${e.message} $e")
        }
    }

    //make sure to shut down logcat when the app exits too

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun lines(minLogLevel: String, userId: String): Flow<String> {
        return flow {
            Logger.d("${context()} Starting 'adb logcat'")

            val logcat = try {
                Command("adb")
                    .args(
                        listOf("logcat", "-v", "brief", userId, minLogLevel)
                    )
                    .stdout(Stdio.Pipe)
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
                        while (isActive) {
                            val line = stdoutReader.readLine()

                            if (line != null) {
                                send(line)
                            } else {
                                Logger.d(">>>>>>>>>>>>>>>>> stop break $isActive")
                                close()
                                break
                            }
                        }
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
    }

    override suspend fun userIdFor(packageName: String) = withContext(dispatcherIo) {
        val UID_CONTEXT = """Packages:\R\s+Package\s+\[$packageName]\s+\(.*\):\R\s+(?:appId|userId)=(\d*)""".toRegex()

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

            //Logger.d("${context()} !Emulator $name")

            emit(running)

            delay(1000L)
        }
    }

    //adb -s emulator-5554 emu avd name
    override suspend fun isShellAvailable(): Boolean {
        return withTimeout(1000) {
            val s = try {
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
