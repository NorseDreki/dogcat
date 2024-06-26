/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.logger

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext

@OptIn(ExperimentalStdlibApi::class)
suspend fun context() = "[${currentCoroutineContext()[CoroutineDispatcher]}]"
