plugins {
    id("com.android.test")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "dev.jspade.mybriefcase.bookmarks.baselineprofile"
    compileSdk = 36
    targetProjectPath = ":app"

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions.managedDevices.localDevices {
        create("pixel6Api31") {
            device = "Pixel 6"
            apiLevel = 31
            systemImageSource = "aosp"
        }
    }
}

baselineProfile {
    managedDevices += "pixel6Api31"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.benchmark.macro.junit4)
}
