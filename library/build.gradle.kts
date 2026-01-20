import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.vanniktech.mavenPublish)
}

// SigmaDelta publishing configuration
group = "be.sigmadelta.simplek"
val libraryVersion = "1.0.0"
val kotlinVersion = libs.versions.kotlin.get()
version = "$libraryVersion-$kotlinVersion"

// XCFramework name for SPM distribution
val xcframeworkName = "SimpleK"

kotlin {
    // JVM target for desktop applications (Windows, macOS, Linux)
    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
        publishLibraryVariants("release")
    }

    // iOS targets with XCFramework for SPM distribution
    val xcf = XCFramework(xcframeworkName)

    iosArm64 {
        binaries.framework {
            baseName = xcframeworkName
            isStatic = true
            xcf.add(this)
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcframeworkName
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(libs.uuid)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        // Desktop (JVM) specific dependencies
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "be.sigmadelta.simplek"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    // Publish to Maven Central: ./gradlew :library:publishToMavenCentral
    publishToMavenCentral()

    // Sign all publications (requires GPG key configuration)
    signAllPublications()

    // Artifact coordinates: be.sigmadelta.simplek:simplek:1.0.0-{kotlinVersion}
    coordinates(group.toString(), "simplek", version.toString())

    pom {
        name = "SimpleK"
        description = "A smooth, animated Kanban board library for Compose Multiplatform with drag-and-drop, zoom-out navigation, and customizable themes"
        inceptionYear = "2025"
        url = "https://github.com/sigmadeltasoftware/SimpleK"

        licenses {
            license {
                name = "Sigma Delta License"
                url = "https://github.com/sigmadeltasoftware/SimpleK/blob/main/LICENSE"
            }
        }

        developers {
            developer {
                id = "sigmadelta"
                name = "SigmaDelta Software"
                url = "https://sigmadelta.be"
            }
        }

        scm {
            url = "https://github.com/sigmadeltasoftware/SimpleK"
            connection = "scm:git:git://github.com/sigmadeltasoftware/SimpleK.git"
            developerConnection = "scm:git:ssh://git@github.com/sigmadeltasoftware/SimpleK.git"
        }
    }
}

// XCFramework tasks for Swift Package Manager (SPM) distribution
tasks.register("buildXCFrameworkForSPM") {
    group = "distribution"
    description = "Builds the XCFramework for Swift Package Manager distribution"
    dependsOn("assemble${xcframeworkName}XCFramework")

    doLast {
        val xcframeworkDir = layout.buildDirectory.dir("XCFrameworks/release").get().asFile
        println("XCFramework built at: ${xcframeworkDir.absolutePath}/$xcframeworkName.xcframework")
    }
}

tasks.register<Exec>("zipXCFramework") {
    group = "distribution"
    description = "Creates a ZIP of the XCFramework for distribution"
    dependsOn("buildXCFrameworkForSPM")

    val xcframeworkDir = layout.buildDirectory.dir("XCFrameworks/release").get().asFile
    workingDir = xcframeworkDir
    commandLine("zip", "-r", "$xcframeworkName.xcframework.zip", "$xcframeworkName.xcframework")

    doLast {
        println("XCFramework ZIP created at: ${xcframeworkDir.absolutePath}/$xcframeworkName.xcframework.zip")
        println("To get the checksum for Package.swift, run:")
        println("  swift package compute-checksum ${xcframeworkDir.absolutePath}/$xcframeworkName.xcframework.zip")
    }
}

// Dokka configuration temporarily disabled
// TODO: Re-enable when Dokka 2.x migration is complete
// Run: ./gradlew :library:dokkaHtml
// Output: library/build/dokka/html/index.html
