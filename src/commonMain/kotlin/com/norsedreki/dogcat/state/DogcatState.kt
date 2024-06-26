/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat.state

import com.norsedreki.dogcat.LogLine
import kotlinx.coroutines.flow.Flow

sealed interface DogcatState {

    data class Active(
        val lines: Flow<IndexedValue<LogLine>>,
        val filters: Flow<LogFilters>,
        val device: Device
    ) : DogcatState

    data object Inactive : DogcatState
}

data class Device(val label: String, val isOnline: Flow<Boolean>)
