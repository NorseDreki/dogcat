/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.app

object AppConfig {

    const val INPUT_KEY_DELAY_MILLIS = 30L

    const val DEFAULT_TAG_WIDTH = 23

    const val MAX_TAG_WIDTH = 50

    const val APP_LOG_FILENAME = "debug.log"

    const val COMMAND_TIMEOUT_MILLIS = 5000L

    const val DEVICE_POLLING_PERIOD_MILLIS = 1500L

    const val TAG_COLOR_PAIR_OFFSET = 100

    const val LOG_LINES_VIEW_BOTTOM_MARGIN = 4

    const val STATUS_VIEW_AUTOSCROLL_LEFT_MARGIN = 15

    const val STATUS_VIEW_BOTTOM_MARGIN = 2

    const val LOG_LEVEL_WIDTH = 1 + 3 + 1 // space, level, space

    const val DEFAULT_LOCALE = "en_US.UTF-8"

    const val EXIT_CODE_ERROR = 1
}
