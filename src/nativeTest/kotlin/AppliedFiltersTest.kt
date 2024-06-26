/*
 * SPDX-FileCopyrightText: Copyright (c) 2024, Alex Dmitriev <mr.alex.dmitriev@icloud.com> and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.norsedreki.dogcat.LogFilter
import io.kotest.matchers.collections.shouldContain
import kotlin.test.Test

class AppliedFiltersTest {

    @Test
    fun `should contain one instance per filter`() {
        val substring = LogFilter.Substring("111")
        val substring2 = LogFilter.Substring("222")
        val filters = mutableSetOf<LogFilter?>()

        // filters.re
        filters.add(substring)
        filters.remove(substring2)
        filters.add(null)

        println(filters)
        filters shouldContain null
    }
}
