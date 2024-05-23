/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
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

class AdbShell(
    private val dispatcherIo: CoroutineDispatcher,
) : Shell {

    private lateinit var adbDevice: String

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun logLines(minLogLevel: String, appId: String): Flow<String> =
        flow {
                Logger.d("${context()} Starting ADB Logcat")

                val logcat =
                    callWithTimeout("Unable to initiate ADB Logcat for log line retrieval") {
                        Command("adb")
                            .args(
                                listOf("-s", adbDevice, "logcat", "-v", "brief", appId, minLogLevel),
                            )
                            .stdout(Pipe)
                            .stderr(Pipe)
                            .spawn()
                    }

                val stdoutReader =
                    logcat.bufferedStdout()
                        ?: throw DogcatException(
                            "Dependent library error: Unable to obtain STDOUT from Logcat",
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
                        Logger.d("Unable to process all elements in 'lines' channel: $e")
                    } finally {
                        Logger.d("COMPLETION (0): Resource cleanup post log line consumption")

                        logcat.shutdownSafely()
                        cancel()
                    }
                }
            }
            .onCompletion { cause ->
                Logger.d(
                    "${context()} COMPLETION (1): ADB logcat has terminated, possibly due to an exception: $cause",
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
            callWithTimeout("Unable to initiate ADB to retrieve a dump of installed applications") {
                Command("adb")
                    .args(listOf("-s", adbDevice, "shell", "dumpsys", "package"))
                    .arg(packageName)
                    .stdout(Pipe)
                    .output()
            }

        val appId =
            output.stdout?.let { out ->
                val match = appIdContext.find(out)

                match?.let {
                    val (id) = it.destructured
                    id
                }
            }

        return appId
            ?: throw DogcatException(
                "Unable to find App ID for the package '$packageName'. The package appears to be not installed " +
                    "on the device.",
            )
    }

    override suspend fun foregroundPackageName(): String {
        val child =
            callWithTimeout("Unable to initiate ADB to retrieve a dump of running activities") {
                Command("adb")
                    .args(listOf("-s", adbDevice, "shell", "dumpsys", "activity", "activities"))
                    .stdout(Pipe)
                    .spawn()
            }

        var packageName: String? = null
        val stdoutReader =
            child.bufferedStdout()
                ?: throw DogcatException(
                    "Dependent library error: Unable to obtain STDOUT from ADB Logcat",
                )

        while (coroutineContext.isActive) {
            val line = stdoutReader.readLine() ?: break

            val packageNamePattern =
                """^ +ResumedActivity: +ActivityRecord\{[^ ]* [^ ]* ([^ ^/]*).*$""".toRegex()

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
                "Unable to locate foreground activity. Consider executing without the '--current' argument",
            )
    }

    override suspend fun deviceName(): String {
        val name =
            callWithTimeout("Unable to initiate ADB to retrieve a name for '$adbDevice'") {
                Command("adb")
                    .args(listOf("-s", adbDevice, "emu", "avd", "name"))
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
            callWithTimeout("Unable to initiate ADB and clear the log source") {
                Command("adb").args(listOf("-s", adbDevice, "logcat", "-c")).status()
            }

        if (exitCode != 0) {
            throw DogcatException("Unable to clear ADB Logcat logs, exit code: $exitCode")
        }

        Logger.d("${context()} Exit code for command 'adb logcat -c': $exitCode")
    }

    override suspend fun firstRunningDevice(): String {
        val output =
            callWithTimeout("Unable to initiate ADB to retrieve a list of running devices") {
                Command("adb").args(listOf("devices")).output()
            }

        val device =
            output.stdout?.let { out ->
                out.lines().firstNotNullOfOrNull {
                    val parts = it.split("\t")

                    when {
                        parts.size < 2 -> null
                        parts[1] == "device" -> parts[0]
                        parts[1] == "unauthorized" ->
                            throw DogcatException(
                                "Authorization pending. Please check your device screen and approve " +
                                    "the USB debugging request",
                            )

                        parts[1] == "offline" ->
                            throw DogcatException(
                                "Device has been detected, but it is currently offline",
                            )

                        else -> null
                    }
                }
            }
                ?: throw DogcatException(
                    "No device is currently online. Please start an emulator or connect a device via USB or WiFi",
                )

        return device
    }

    override suspend fun validateShellOrThrow() {
        val m =
            "Android Debug Bridge (ADB), a component of Android SDK, is not found. Please install Android SDK " +
                "and ensure its installation location is added to the \$PATH environment variable."

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
                "Android Debug Bridge (ADB) was found but it returned an error: $returnStatus.",
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
                "An exception was thrown by the Kommand library while attempting " +
                    "to terminate a process: ${e.message} $e",
            )
        }
    }
}
