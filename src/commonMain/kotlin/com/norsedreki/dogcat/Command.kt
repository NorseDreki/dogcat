/*
 * SPDX-FileCopyrightText: Copyright (C) 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

import kotlin.reflect.KClass

sealed interface Command {
    sealed interface Start : Command {
        data object PickAllApps : Start

        data object PickForegroundApp : Start

        data class PickAppPackage(val packageName: String) : Start
    }

    data class FilterBy(val filter: LogFilter) : Command

    data class ResetFilter(val filterClass: KClass<out LogFilter>) : Command

    data object ClearLogs : Command

    data object Stop : Command
}
