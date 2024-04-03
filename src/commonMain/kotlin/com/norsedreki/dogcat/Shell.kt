/*
 * SPDX-FileCopyrightText: Copyright (C) 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

import kotlinx.coroutines.flow.Flow

interface Shell {

    fun logLines(minLogLevel: String, appId: String): Flow<String>

    fun isDeviceOnline(): Flow<Boolean>

    suspend fun appIdFor(packageName: String): String

    suspend fun deviceName(): String

    suspend fun foregroundPackageName(): String

    suspend fun clearLogLines()

    suspend fun firstRunningDevice(): String

    suspend fun validateShellOrThrow()
}
