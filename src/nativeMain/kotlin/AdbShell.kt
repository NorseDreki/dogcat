import AppConfig.COMMAND_TIMEOUT_MILLIS
import com.kgit2.kommand.exception.KommandException
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import dogcat.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import logger.Logger
import logger.context

class AdbShell(
    private val dispatcherIo: CoroutineDispatcher = Dispatchers.IO
) : Shell {
    override fun lines(minLogLevel: String, userId: String): Flow<String> {
        val logcat = Command("adb")
            .args(
                listOf("logcat", "-v", "brief", userId, minLogLevel)
            )
            .stdout(Stdio.Pipe)
            .spawn() // throws ex

        return flow {
            Logger.d("${context()} Starting adb logcat")


            val stdoutReader = logcat.bufferedStdout()!! //do not assert

            try {
                coroutineScope {
                    val lineChannel = Channel<String>()

                    try {
                    val j = launch(dispatcherIo) {
                        while (currentCoroutineContext().isActive) {
                            val line = stdoutReader.readLine()
                            if (line != null) {
                                lineChannel.send(line)
                            } else {
                                break
                            }
                        }
                        Logger.d(">>>>>>>>>>>>>>>>> stop launch")
                    }
                    } catch (e: CancellationException) {
                        Logger.d(">>>>>>>>>>>>>>>>> MTHRFRK! $e  $this") //never appeared
                        logcat.kill()

                        //j.cancel()
                        //} catch (e: CancellationException) {
                        //  Logger.d(">>>>>>>>>>>>>>>>> CancellationException   !!!!!!!!!!! Cancellation!  $this")
                    }

                    //throw RuntimeException("111111!")

                    try {
                        while (true) {
                            //yield()
                            //Logger.d("Reading line $this")
                            val line = lineChannel.receive() ?: break
                            //Logger.d("Read line $this")
                            emit(line)
                            //Logger.d("Emitted $this")

                            /*if (!currentCoroutineContext().isActive) {
                                throw RuntimeException("Coroutine was cancelled[[[[[")
                            }*/
                        }
                    } catch (e: CancellationException) {
                        Logger.d(">>>>>>>>>>>>>>>>> inner catch $e  $this")
                        logcat.kill()

                        //j.cancel()
                    //} catch (e: CancellationException) {
                      //  Logger.d(">>>>>>>>>>>>>>>>> CancellationException   !!!!!!!!!!! Cancellation!  $this")
                    } finally {
                        Logger.d(">>>>>>>>>>>>>>>>> finally   !!!!!!!!!n  $this")
                        //delay(1000)
                        //throw RuntimeException("2113242314")

                    }
                    /*catch (e: KommandException) {
                        Logger.d("!!!!!!!!!!! KommandEx! $e")
                    } catch (e: RuntimeException) {
                        Logger.d(
                            "!!!!!!!!!!! Runtime! ${e.message} ${context()}"
                        )
                    }*/
                }
            } catch (e: RuntimeException) {
                Logger.d(">>>>>>>>>>>>>>>>> outer   Caught!  $e $this")
                //logcat.kill()
            }
            /*try {
                while (currentCoroutineContext().isActive) {
                    yield()
                    Logger.d("Reading line $this")
                    val line = stdoutReader.readLine() ?: break
                    Logger.d("Read line $this")
                    emit(line)
                    Logger.d("Emitted $this")
                }
            } catch (e: CancellationException) {
                Logger.d("!!!!!!!!!!! Cancellation! $e $this")
            } catch (e: KommandException) {
                Logger.d("!!!!!!!!!!! KommandEx! $e")
            } catch (e: RuntimeException) {
                Logger.d(
                    "!!!!!!!!!!! Runtime! ${e.message} ${context()}"
                )
            }*/

            Logger.d(">>>>>>>>>>>>>>>>>>>>>>>> ${context()} !!!!!!!!! Killing logcat ${currentCoroutineContext().isActive}")
            // tried timeout, need async IO so badly
            // command would be killed when next line appears.
            // also, no leftover adb upon app exit
            //logcat.kill()
        }
            .onCompletion {
                Logger.d(">>>>>>>>>>>>  ${context()} !!!!!!!!! Killing logcat on Completion ${currentCoroutineContext().isActive}")
                //logcat.kill()
            }
            .catch { cause ->
                if (cause is CancellationException) {
                    // Handle cancellation
                    Logger.d("|||||||||||||||||||||||||||||||||||||||||||||||||||  Flow was cancelled, cleaning up resources...")
                    // Clean up resources here
                } else {
                    // Handle other exceptions
                    Logger.d("||||||||||||||||||||||||||||||||||||||||||||| An error occurred: $cause")
                }
            }
            
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

            //Logger.d("${context()} !Emulator $name")

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
