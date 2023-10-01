import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "1.9.0"
}

group = "com.norsedreki"
version = "0.1"

repositories {
    mavenCentral()
}

/*
kotlin {
    jvm()

    js(IR) {
        browser()
        nodejs()
    }

    linuxX64()

    mingwX64()

    macosX64()
    macosArm64()
    ios() // shortcut for iosArm64, iosX64

    // Native targets all extend commonMain and commonTest.
    //
    // Some targets (ios, tvos, watchos) are shortcuts provided by the Kotlin DSL, that
    // provide additional targets, except for 'simulators' which must be defined manually.
    // https://kotlinlang.org/docs/multiplatform-share-on-platforms.html#use-target-shortcuts
    //
    // common
    // └── native
    //     ├── linuxX64
    //     ├── mingwX64
    //     ├── macosX64
    //     ├── macosArm64
    //     └── ios (shortcut)
    //         ├── iosArm64
    //         └── iosX64

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting
        val commonTest by getting

        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest) }

        // Linux
        val linuxX64Main by getting { dependsOn(nativeMain) }
        val linuxX64Test by getting { dependsOn(nativeTest) }

        // Windows - MinGW
        val mingwX64Main by getting { dependsOn(nativeMain) }
        val mingwX64Test by getting { dependsOn(nativeTest) }

        // Apple - macOS
        val macosArm64Main by getting { dependsOn(nativeMain) }
        val macosArm64Test by getting { dependsOn(nativeTest) }

        val macosX64Main by getting { dependsOn(nativeMain) }
        val macosX64Test by getting { dependsOn(nativeTest) }

        // Apple - iOS
        val iosMain by getting { dependsOn(nativeMain) }
        val iosTest by getting { dependsOn(nativeTest) }
    }
}
*/

@OptIn(ExperimentalKotlinGradlePluginApi::class)
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

    //targetHierarchy.default()

    val testFrameworkAttribute = Attribute.of("com.example.testFramework", String::class.java)
    //macosX64() // { attributes.attribute(testFrameworkAttribute, "junit") }
    //mingwX64()
    linuxX64("native") {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
        compilations["main"].cinterops {
            val ncurses by creating {
            }
        }
    }
    //linuxX64()

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
    /*val darwinTargets = listOf(
        macosX64() { attributes.attribute(testFrameworkAttribute, "junit") },
    )*/



    /*val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }*/

    /*targets.withType(KotlinNativeTarget::class.java) {
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
    }*/

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
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.kodein.di:kodein-di:7.20.2")
            }
        }


        val nativeMain by getting {
            //dependsOn(commonMain)

            dependencies {
                implementation("com.kgit2:kommand:1.0.2")
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

        /*val macosX64Main by getting { dependsOn(nativeMain) }
        val macosX64Test by getting { dependsOn(nativeTest) }*/

        /*val linuxX64Main by getting { dependsOn(nativeMain) }
        val linuxX64Test by getting { dependsOn(nativeTest) }*/


        /*val darwinMain by creating {
            dependsOn(nativeMain)
        }
        darwinTargets.forEach {
            getByName("${it.targetName}Main") {
                dependsOn(darwinMain)
            }
        }*/
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

/*
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
*/
