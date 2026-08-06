import mihon.buildlogic.AndroidConfig
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("mihon.kmp.library")
    kotlin("multiplatform")
    alias(kotlinx.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "tachiyomi.presentation.core"
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
                api(projects.core.common)
                api(projects.i18n)

                implementation(platform(compose.bom))

                // Compose
                implementation(compose.activity)
                implementation(compose.foundation)
                implementation(compose.material3.core)
                implementation(compose.material.icons)
                implementation(compose.animation)
                implementation(compose.animation.graphics)
                implementation(compose.ui.tooling)
                implementation(compose.ui.tooling.preview)
                implementation(compose.ui.util)

                implementation(androidx.paging.runtime)
                implementation(androidx.paging.compose)
                implementation(androidx.lifecycle.runtime.compose)
                implementation(kotlinx.immutables)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }
}
