/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

import com.norsedreki.logger.Logger

interface LogLineParser {
    fun parse(line: String): LogLine
}

class LogLineBriefParser : LogLineParser {
    private val briefPattern = """^([A-Z])/(.+?)\( *(\d+)\): (.*?)$""".toRegex()

    override fun parse(line: String): LogLine {
        val match = briefPattern.matchEntire(line)

        return if (match != null) {
            // As an option, use 'named capturing groups' feature of 1.9, but it looks more verbose
            val (level, tag, owner, message) = match.destructured

            Brief(
                LogLevel.valueOf(level),
                tag,
                owner,
                message,
            )
        } else {
            Logger.d("[LogLineParser] Unparseable log line: '$line'")

            Unparseable(line)
        }
    }
}
