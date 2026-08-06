import mihon.buildlogic.AndroidConfig
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("mihon.kmp.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    android {
        namespace = "eu.kanade.tachiyomi.core.common"
        compileSdk = AndroidConfig.COMPILE_SDK
        minSdk = AndroidConfig.MIN_SDK
        withJava()
        withHostTestBuilder { }
    }

    sourceSets {
        getByName("androidMain") {
            dependencies {
                implementation(projects.i18n)

                api(libs.logcat)

                api(libs.rxjava)

                api(libs.okhttp.core)
                api(libs.okhttp.logging)
                api(libs.okhttp.brotli)
                api(libs.okhttp.dnsoverhttps)
                api(libs.okio)

                implementation(libs.image.decoder)

                implementation(libs.unifile)
                implementation(libs.libarchive)

                implementation(androidx.webkit)

                api(kotlinx.coroutines.core)
                api(kotlinx.serialization.json)
                api(kotlinx.serialization.json.okio)

                // PreferenceManager is only needed inside this module's AndroidPreferenceStore implementation.
                implementation(libs.preferencektx)

                implementation(libs.jsoup)
                compileOnly(libs.jspecify)

                // Sort
                implementation(libs.natural.comparator)

                // JavaScript engine
                implementation(libs.bundles.js.engine)

                // FFmpeg-kit
                implementation(aniyomilibs.ffmpeg.kit)

                // TorrServer
                implementation(aniyomilibs.torrserver)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}
