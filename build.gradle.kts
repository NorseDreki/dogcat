import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "1.9.0"
}

group = "com.norsedreki"
version = "0.1"

repositories {
    mavenCentral()
}

kotlin {
    /*jvm()
    js {
        nodejs()
        browser()
    }

    linuxX64()
    mingwX64()
    macosX64()
    macosArm64()*/

    val testFrameworkAttribute = Attribute.of("com.example.testFramework", String::class.java)

    /*linuxX64("1") {
        //attributes.attribute(testFrameworkAttribute, "junit")
        binaries {
            executable(listOf(DEBUG)) {
                entryPoint = "main"
            }
            executable(listOf(RELEASE)) {
                entryPoint = "main"
            }
        }
        compilations["main"].cinterops {
            val ncurses by creating {
            }
        }
    }*/

    /*macosX64("nativeMac")
    linuxX64("nativeLin")
    mingwX64("nativeWin")

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }*/
    /*val linuxTargets = listOf(
        linuxX64()
    )*/

   // val linuxTargets = listOf(
        //linuxArm64(),
        /*linuxX64() {
            binaries {
                executable(listOf(DEBUG)) {
                    entryPoint = "main"
                }
                executable(listOf(RELEASE)) {
                    entryPoint = "main"
                }
            }
            *//*compilations["main"].cinterops {
                val ncurses by creating {
                }
            }*//*
        },*/
        //mingwX64()
   // )
    val darwinTargets = listOf(
        macosX64() { attributes.attribute(testFrameworkAttribute, "junit") },
    )



    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    targets.withType(KotlinNativeTarget::class.java) {
        println("$this")
        compilations["main"].cinterops {
            val ncurses by creating {
            }
        }
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    /*nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
        compilations["main"].cinterops {
            val ncurses by creating {
            }
        }
    }*/

    /*val testFrameworkAttribute = Attribute.of("com.example.testFramework", String::class.java)
    jvm("junit") {
        attributes.attribute(testFrameworkAttribute, "junit")
    }
    jvm("testng") {
        attributes.attribute(testFrameworkAttribute, "testng")
    }*/
    //The consumer has to add the attribute to a single target where the ambiguity arises.

    sourceSets {
        val nativeMain by getting {

            dependencies {
                implementation("com.kgit2:kommand:1.0.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.kodein.di:kodein-di:7.20.2")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
            }
        }
        val nativeTest by getting {
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
        val darwinMain by creating {
            dependsOn(nativeMain)
        }
        darwinTargets.forEach {
            getByName("${it.targetName}Main") {
                dependsOn(darwinMain)
            }
        }
        /*val linuxMain by creating {
            dependsOn(nativeMain)
        }
        linuxTargets.forEach {

            getByName("${it.targetName}Main") {
                dependsOn(linuxMain)
            }
        }*/
        /*val linuxX64 by getting {
            dependsOn(nativeMain)
        }*/
        /*val nativeMacMain by getting {
            dependsOn(nativeMain)
        }
        val nativeLinMain by getting {
            dependsOn(nativeMain)
        }*/

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

    //afterEvaluate {
        /*afterEvaluate {
            tasks.named("compileNativeMainKotlinMetadata") {
                enabled = false
            }
        }*/
    //}
}

afterEvaluate {
    afterEvaluate {
        tasks.configureEach {
            if (
                name.startsWith("compile")
                && name.endsWith("KotlinMetadata")
            ) {
                println("disabling :$name")
                enabled = false
            }
        }
    }
}
