/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app

import com.norsedreki.dogcat.app.AppConfig.DEFAULT_TAG_WIDTH
import com.norsedreki.dogcat.app.AppConfig.MAX_TAG_WIDTH
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.optional

class AppArguments(
    private val parser: ArgParser,
) {

    class ValidationException(override val message: String) : RuntimeException(message)

    val packageName by
        parser
            .argument(
                ArgType.String,
                fullName = "package name",
                description =
                "Display logs for a dedicated application, specified by its package name. " +
                    "For example, 'com.google.android.apps.messaging'.",
            )
            .optional()

    val current by
        parser.option(
            ArgType.Boolean,
            shortName = "c",
            description = "Display logs for the application currently running in the foreground.",
        )

    val lineNumbers by
        parser.option(
            ArgType.Boolean,
            shortName = "ln",
            description = "Display line numbers for log lines, embedded within the message body.",
        )

    val tagWidth by
        parser.option(
            ArgType.Int,
            shortName = "tw",
            description =
            "Specify the width for displaying log tags. Accepts values " +
                "between 1 and $MAX_TAG_WIDTH. Default value is $DEFAULT_TAG_WIDTH.",
        )

    val version by
        parser.option(
            ArgType.Boolean,
            shortName = "v",
            description = "Display the version of this application.",
        )

    fun validate(args: Array<String>) {
        parser.parse(args)

        if (packageName != null && current != null) {
            throw ValidationException(
                "The 'package name' and '--current' arguments cannot be used simultaneously.",
            )
        }

        tagWidth?.let {
            if (tagWidth !in 1..MAX_TAG_WIDTH) {
                throw ValidationException(
                    "The tag width must be a value between 1 and $MAX_TAG_WIDTH.",
                )
            }
        }
    }
}
