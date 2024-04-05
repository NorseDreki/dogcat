/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app

import com.kgit2.kommand.exception.KommandException
import com.kgit2.kommand.process.Child
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio.Pipe
import com.norsedreki.dogcat.DogcatException
import com.norsedreki.dogcat.Shell
import com.norsedreki.dogcat.app.AppConfig.COMMAND_TIMEOUT_MILLIS
import com.norsedreki.dogcat.app.AppConfig.DEVICE_POLLING_PERIOD_MILLIS
import com.norsedreki.logger.Logger
import com.norsedreki.logger.context
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AdbShell(private val dispatcherIo: CoroutineDispatcher) : Shell {

    private lateinit var adbDevice: String

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun logLines(minLogLevel: String, appId: String): Flow<String> =
        flow {
                Logger.d("${context()} Starting ADB Logcat")

                val logcat =
                    callWithTimeout("Failed to start ADB Logcat to get log lines") {
                        Command("adb")
                            .args(
                                listOf(
                                    "-s",
                                    adbDevice,
                                    "logcat",
                                    "-v",
                                    "brief",
                                    appId,
                                    minLogLevel
                                ),
                            )
                            .stdout(Pipe)
                            .stderr(Pipe)
                            .spawn()
                    }

                val stdoutReader =
                    logcat.bufferedStdout()
                        ?: throw DogcatException(
                            "Error in a dependent library, could not get STDOUT of ADB Logcat"
                        )

                coroutineScope {
                    val lines =
                        produce(dispatcherIo) {
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
                        lines.consumeEach { emit(it) }
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
                Logger.d(
                    "${context()} COMPLETION (1): ADB logcat has terminated, maybe with exception: $cause"
                )
            }
            .flowOn(dispatcherIo)

    override fun isDeviceOnline(): Flow<Boolean> =
        flow {
                repeat(Int.MAX_VALUE) {
                    try {
                        firstRunningDevice()
                        emit(true)
                    } catch (e: DogcatException) {
                        emit(false)
                    }

                    /*val name = callWithTimeout(
                        "Failed to get running status of device, " +
                                "make sure an emulator or a device is connected"
                    ) {

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

                    val running = name?.contains("running") ?: false
                    emit(running)*/

                    delay(DEVICE_POLLING_PERIOD_MILLIS)
                }
            }
            .flowOn(dispatcherIo)

    override suspend fun appIdFor(packageName: String): String {
        val appIdContext =
            """Packages:\R\s+Package\s+\[$packageName]\s+\(.*\):\R\s+(?:appId|userId)=(\d*)"""
                .toRegex()

        val output =
            callWithTimeout("Failed to start ADB to get a dump of installed applications") {
                Command("adb")
                    .args(
                        listOf("-s", adbDevice, "shell", "dumpsys", "package"),
                    )
                    .arg(packageName)
                    .stdout(Pipe)
                    .output()
            }

        val appId =
            output.stdout?.let {
                val match = appIdContext.find(it)

                match?.let {
                    val (id) = it.destructured
                    id
                }
            }

        return appId
            ?: throw DogcatException(
                "App ID is not found for the package '$packageName'. " +
                    "Looks like this package is not installed on device.",
            )
    }

    override suspend fun foregroundPackageName(): String {
        val packageNamePattern =
            """^ +ResumedActivity: +ActivityRecord\{[^ ]* [^ ]* ([^ ^/]*).*$""".toRegex()

        val child =
            callWithTimeout("Failed to start ADB to get a dump of running activities") {
                Command("adb")
                    .args(
                        listOf("-s", adbDevice, "shell", "dumpsys", "activity", "activities"),
                    )
                    .stdout(Pipe)
                    .spawn()
            }
        var packageName: String? = null
        val stdoutReader =
            child.bufferedStdout()
                ?: throw DogcatException(
                    "Error in a dependent library, could not get STDOUT of ADB Logcat"
                )

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

        return packageName
            ?: throw DogcatException(
                "Failed to find foreground activity, " +
                    "consider running without '--current' argument instead",
            )
    }

    override suspend fun deviceName(): String {
        val name =
            callWithTimeout("Failed to start ADB to get a name for '$adbDevice'") {
                Command("adb")
                    .args(
                        listOf("-s", adbDevice, "emu", "avd", "name"),
                    )
                    .stdout(Pipe)
                    .output()
                    .stdout
                    ?.lines()
                    ?.first()
            }

        val result =
            if (!name.isNullOrEmpty()) {
                name
            } else {
                adbDevice
            }

        return result
    }

    override suspend fun clearLogLines() {
        val exitCode =
            callWithTimeout("Failed to start ADB and clear logs") {
                Command("adb")
                    .args(
                        listOf("-s", adbDevice, "logcat", "-c"),
                    )
                    .status()
            }

        if (exitCode != 0) {
            throw DogcatException("Failed to clear logs of Logcat, exit code: $exitCode")
        }

        Logger.d("${context()} Exit code for 'adb logcat -c': $exitCode")
    }

    override suspend fun firstRunningDevice(): String {
        val output =
            callWithTimeout("Failed to start ADB to get a list of running devices") {
                Command("adb")
                    .args(
                        listOf("devices"),
                    )
                    .output()
            }

        val device =
            output.stdout?.let {
                it.lines().firstNotNullOfOrNull {
                    val parts = it.split("\t")

                    val d =
                        when {
                            parts.size < 2 -> null
                            parts[1] == "device" -> parts[0]
                            parts[1] == "unauthorized" ->
                                throw DogcatException(
                                    "Pending authorization, please refer to your device screen and " +
                                        "grant a request for USB debugging",
                                )
                            parts[1] == "offline" ->
                                throw DogcatException("Device is detected, but is in offline state")
                            else -> null
                        }
                    d
                }
            }
                ?: throw DogcatException(
                    "No device is online, please start an emulator or connect a device via USB or WiFi"
                )

        return device
    }

    override suspend fun validateShellOrThrow() {
        val m =
            "Android Debug Bridge (ADB), a part of Android SDK, is not found. Please install Android SDK " +
                "and make sure to add its installation location to the \$PATH environment variable"

        val returnStatus =
            callWithTimeout(m) {
                val status =
                    Command("adb")
                        .args(
                            listOf("version"),
                        )
                        .stdout(Pipe)
                        .status()

                // maybe just invoking this would be enough for ADB test
                adbDevice = firstRunningDevice()

                status
            }

        if (returnStatus != 0) {
            throw DogcatException(
                "Android Debug Bridge (ADB) is found but returned an error: $returnStatus."
            )
        }
    }

    private suspend fun <T> callWithTimeout(
        errorMessage: String,
        command: suspend CoroutineScope.() -> T,
    ): T =
        try {
            withContext(dispatcherIo) { withTimeout(COMMAND_TIMEOUT_MILLIS) { command() } }
        } catch (e: KommandException) {
            throw DogcatException(errorMessage, e)
        } catch (e: TimeoutCancellationException) {
            throw DogcatException(errorMessage, e)
        }

    private fun Child.shutdownSafely() {
        try {
            kill()
        } catch (e: KommandException) {
            Logger.d(
                "Kommand library has thrown an exception when trying to kill a process: ${e.message} $e"
            )
        }
    }
}
