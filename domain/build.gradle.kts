import mihon.buildlogic.AndroidConfig
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("mihon.kmp.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    android {
        namespace = "com.tadami.aurora.domain"
        compileSdk = AndroidConfig.COMPILE_SDK
        minSdk = AndroidConfig.MIN_SDK
        withJava()
        withHostTestBuilder { }

        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-rules.pro")
            }
        }
    }

    sourceSets {
        getByName("androidMain") {
            dependencies {
                implementation(projects.sourceApi)
                implementation(projects.core.common)

                implementation(libs.jsoup)
                compileOnly(libs.jspecify)
                implementation(kotlinx.bundles.coroutines)
                implementation(kotlinx.bundles.serialization)

                implementation(libs.unifile)

                // AndroidX Paging for PagingSource
                api(libs.paging.common)

                compileOnly(libs.compose.stablemarker)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test)
                implementation(kotlinx.coroutines.test)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}
