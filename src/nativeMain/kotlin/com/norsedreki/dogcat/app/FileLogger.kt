/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app

import com.norsedreki.dogcat.DogcatException
import com.norsedreki.dogcat.app.AppConfig.APP_LOG_FILENAME
import com.norsedreki.logger.CanLog
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen
import platform.posix.fprintf

@OptIn(ExperimentalForeignApi::class)
class FileLogger : CanLog {

    // I/O is not cool for field initializers, but would be OK when debugging
    private val file: CPointer<FILE> =
        fopen(APP_LOG_FILENAME, "w")
            ?: throw DogcatException("Was not able to open log file for writing.")

    override fun d(line: String) {
        fprintf(file, "$line\n")
        fflush(file)
    }

    override fun close() {
        fflush(file)
        fclose(file)
    }
}
