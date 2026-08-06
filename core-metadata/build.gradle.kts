import mihon.buildlogic.AndroidConfig

plugins {
    id("mihon.kmp.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    android {
        namespace = "tachiyomi.core.metadata"
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
}

dependencies {
    implementation(projects.sourceApi)

    implementation(kotlinx.bundles.serialization)
}
