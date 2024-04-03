/*
 * SPDX-FileCopyrightText: Copyright (C) 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

object DogcatConfig {

    // Lower the number, less memory consumption. Also note that NCurses pad, which is the current
    // implementation
    // to hold and scroll log, can handle 32767 lines at max.
    const val MAX_LOG_LINES = 2000

    val DEFAULT_MIN_LOG_LEVEL = LogLevel.V
}
