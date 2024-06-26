/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.norsedreki.dogcat

class DogcatException(
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
