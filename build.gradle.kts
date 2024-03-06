plugins {
    kotlin("multiplatform") version "1.9.22"
}

group = "com.norsedreki"
version = "0.1"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
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
                entryPoint = "com.norsedreki.dogcat.app.main"
            }
        }
        compilations["main"].cinterops {
            val ncurses by creating {
            }
        }
    }

    val generateBuildConfig by tasks.registering {
        doLast {
            val file = file("src/nativeMain/kotlin/com/norsedreki/dogcat/app/BuildConfig.kt")
            val isDebug = System.getenv("DEBUG")?.toBoolean() ?: false

            file.writeText(
                """
                // This file is generated. Refer to build.gradle.kts to see how.
                package com.norsedreki.dogcat.app
                
                object BuildConfig {
                    const val DEBUG = $isDebug
                }
                """.trimIndent()
            )
        }
    }

    tasks {
        val nativeTargetName = nativeTarget.name.replaceFirstChar { it.uppercaseChar() }

        "compileKotlin$nativeTargetName" {
            dependsOn("generateBuildConfig")
        }
    }

//    applyDefaultHierarchyTemplate()


    sourceSets {
        val commonMain by getting {
            dependencies {
                //extract version numbers to TOML
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                api("org.kodein.di:kodein-di:7.20.2")
            }
        }

        val nativeMain by creating {
            dependencies {
                implementation("com.kgit2:kommand:2.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
            }
        }
        val nativeTest by creating {
            dependencies {
                //or api()??
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

                implementation("io.kotest:kotest-assertions-core:5.6.2")
                implementation("app.cash.turbine:turbine:1.0.0")
                implementation("io.mockative:mockative:1.4.1")
                implementation("org.kodein.di:kodein-di:7.20.2")

                /*implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(Libs.kotest("assertions-core"))*/
            }
        }

        /*val commonTest by getting {
            dependencies {
                api(kotlin("test"))
                api("io.kotest:kotest-assertions-core:4.5.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotest.assertions.core)
            }
        }*/
    }
}
