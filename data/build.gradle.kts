import mihon.buildlogic.AndroidConfig
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("mihon.kmp.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "com.tadami.aurora.data"
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
                implementation(projects.domain)
                implementation(projects.core.common)
                implementation(projects.i18n)

                implementation(libs.bundles.sqldelight)
                implementation(kotlinx.bundles.serialization)
            }
        }
        getByName("androidDebug") {
            kotlin.srcDirs(
                "build/generated/sqldelight/code/Database/debug",
                "build/generated/sqldelight/code/AnimeDatabase/debug",
                "build/generated/sqldelight/code/NovelDatabase/debug",
                "build/generated/sqldelight/code/AchievementsDatabase/debug",
            )
        }
        getByName("androidRelease") {
            kotlin.srcDirs(
                "build/generated/sqldelight/code/Database/release",
                "build/generated/sqldelight/code/AnimeDatabase/release",
                "build/generated/sqldelight/code/NovelDatabase/release",
                "build/generated/sqldelight/code/AchievementsDatabase/release",
            )
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test)
                implementation(kotlinx.coroutines.test)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.okhttp.mockwebserver)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("tachiyomi.data")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(project.file("./src/androidMain/sqldelight"))
            srcDirs.from(project.file("./src/androidMain/sqldelight"))
        }
        create("AnimeDatabase") {
            packageName.set("tachiyomi.mi.data")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(project.file("./src/androidMain/sqldelightanime"))
            srcDirs.from(project.file("./src/androidMain/sqldelightanime"))
        }
        create("NovelDatabase") {
            packageName.set("tachiyomi.novel.data")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(project.file("./src/androidMain/sqldelightnovel"))
            srcDirs.from(project.file("./src/androidMain/sqldelightnovel"))
        }
        create("AchievementsDatabase") {
            packageName.set("tachiyomi.db.achievement")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(project.file("./src/androidMain/sqldelightachievements"))
            srcDirs.from(project.file("./src/androidMain/sqldelightachievements"))
        }
    }
}

tasks.matching {
    it.name == "extractDebugAnnotations" || it.name == "extractReleaseAnnotations"
}.configureEach {
    val variant = name.removePrefix("extract").removeSuffix("Annotations")
    dependsOn(
        "generate${variant}DatabaseInterface",
        "generate${variant}AnimeDatabaseInterface",
        "generate${variant}NovelDatabaseInterface",
        "generate${variant}AchievementsDatabaseInterface",
    )
}
