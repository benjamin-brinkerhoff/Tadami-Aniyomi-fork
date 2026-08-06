import mihon.buildlogic.AndroidConfig

plugins {
    id("mihon.kmp.library")
    kotlin("multiplatform")
    alias(kotlinx.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "tachiyomi.presentation.widget"
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
                implementation(projects.core.common)
                implementation(projects.domain)
                implementation(projects.presentationCore)
                api(projects.i18n)
                api(projects.i18nAniyomi)

                implementation(platform(compose.bom))

                implementation(compose.glance)
                implementation(libs.material)

                implementation(kotlinx.immutables)

                implementation(platform(libs.coil.bom))
                implementation(libs.coil.core)

                api(libs.injekt)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test)
                runtimeOnly(libs.junitPlatformLauncher)
            }
        }
    }
}
