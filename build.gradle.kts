/*
 * SPDX-FileCopyrightText: Copyright (C) 2024 Alex Dmitriev <mr.alex.dmitriev@icloud.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import com.diffplug.spotless.LineEnding

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.spotless)
}

group = "com.norsedreki"

version = "0.9-RC"

repositories { mavenCentral() }

spotless {
    charset("utf-8")
    lineEndings = LineEnding.UNIX

    kotlin {
        target("**/*.kt", "**/*.gradle.kts")
        targetExclude("**/BuildConfig.kt")

        licenseHeaderFile(
            rootProject.file("gradle/license-notice.txt"),
            "^(package|@file|import|plugins|pluginManagement|class)"
        )

        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()

        toggleOffOn()
        indentWithSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("misc") {
        target("**/*.json", "**/*.yaml", "**/*.yml", "**/*.toml")

        indentWithSpaces(2)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")

    val nativeTarget =
        when {
            hostOs.startsWith("Mac OS X") && isArm64 -> macosArm64("nativeMacArm64")
            hostOs.startsWith("Mac OS X") && !isArm64 -> macosX64("nativeMacX64")
            hostOs.startsWith("Linux") && isArm64 -> linuxArm64("nativeLinuxArm64")
            hostOs.startsWith("Linux") && !isArm64 -> linuxX64("nativeLinuxX64")
            isMingwX64 -> mingwX64("nativeWindows")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }

    nativeTarget.apply {
        binaries {
            executable {
                // Do not format
                entryPoint = "com.norsedreki.dogcat.app.main"
            }
        }

        compilations["main"].cinterops {
            val ncurses by creating
        }
    }

    val generateBuildConfig by
        tasks.registering {
            doLast {
                val file = file("src/nativeMain/kotlin/com/norsedreki/dogcat/app/BuildConfig.kt")
                val isDebug = System.getenv("DEBUG")?.toBoolean() ?: false

                file.writeText(
                    """
                    // This file is generated. Refer to build.gradle.kts to see how.
                    package com.norsedreki.dogcat.app

                    object BuildConfig {
                        const val DEBUG = $isDebug
                        const val VERSION = "${project.version}"
                    }

                    """
                        .trimIndent()
                )
            }
        }

    tasks {
        val nativeTargetName = nativeTarget.name.replaceFirstChar { it.uppercaseChar() }

        "compileKotlin$nativeTargetName" {
            // Do not format
            dependsOn("generateBuildConfig")
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.coroutines.core)
                api(libs.kodeinDi)
            }
        }

        val nativeMain by getting {
            dependencies {
                implementation(libs.kommand)
                implementation(libs.kotlinx.cli)
            }
        }

        val nativeTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)
                implementation(libs.kotestAssertionsCore)
                implementation(libs.turbine)
                implementation(libs.mockative)
                implementation(libs.kodeinDi)
            }
        }
    }
}
