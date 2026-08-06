import mihon.buildlogic.AndroidConfig

plugins {
    id("mihon.kmp.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    android {
        namespace = "mihon.core.archive"
        compileSdk = AndroidConfig.COMPILE_SDK
        minSdk = AndroidConfig.MIN_SDK
        withJava()
        withHostTestBuilder { }
    }
}

dependencies {
    implementation(libs.jsoup)
    compileOnly(libs.jspecify)
    implementation(libs.libarchive)
    implementation(libs.unifile)
}
