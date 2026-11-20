plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.compose)
    alias(libs.plugins.ksp)
}
android {
    namespace = "pe.rentamaq"
    compileSdk { version = release(37) { minorApiLevel = 1 } }
    defaultConfig {
        applicationId = "pe.rentamaq"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.material3:material3")
    implementation(libs.activity)
    implementation(libs.viewmodel)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
}
