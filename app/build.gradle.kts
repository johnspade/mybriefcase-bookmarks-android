import com.palantir.gradle.gitversion.VersionDetails

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val versionDetails: groovy.lang.Closure<VersionDetails> by rootProject.extra

val versionTagRegex = Regex("""^v?\d+\.\d+\.\d+$""")

fun computeVersionCode(): Int {
    val details = versionDetails()
    val tag = details.lastTag
    if (tag == null || !tag.matches(versionTagRegex)) return 1
    val parts = tag.removePrefix("v").split(".")
    val major = parts[0].toInt()
    val minor = parts[1].toInt()
    val patch = parts[2].toInt()
    return major * 10000 + minor * 100 + patch
}

fun computeVersionName(): String {
    val details = versionDetails()
    val tag = details.lastTag
    if (tag == null || !tag.matches(versionTagRegex)) return "0.0.0-${details.gitHash}"
    val base = tag.removePrefix("v")
    if (details.commitDistance > 0) return "$base-${details.commitDistance}-${details.gitHash}"
    return base
}

val rustDir = rootProject.layout.projectDirectory.dir("rust")
val jniLibsDir = layout.projectDirectory.dir("src/main/jniLibs")

val cargoNdkAvailable = providers.exec {
    commandLine("which", "cargo-ndk")
    isIgnoreExitValue = true
}.result.map { it.exitValue == 0 }.get()

if (cargoNdkAvailable) {
    val buildRustNativeLibs by tasks.registering(Exec::class) {
        description = "Build Rust FFI native libraries with cargo-ndk"

        outputs.upToDateWhen { false }

        workingDir = rustDir.asFile
        commandLine("cargo", "ndk", "-t", "arm64-v8a", "-t", "x86_64", "-o", jniLibsDir.asFile.absolutePath, "build", "--release")
    }

    tasks.named("preBuild") {
        dependsOn(buildRustNativeLibs)
    }
}

android {
    namespace = "dev.jspade.mybriefcase.bookmarks"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.jspade.mybriefcase.bookmarks"
        minSdk = 24
        targetSdk = 36
        versionCode = computeVersionCode()
        versionName = computeVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    testImplementation(libs.junit)
    testImplementation("androidx.work:work-testing:2.9.1")
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation("net.java.dev.jna:jna:5.13.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}