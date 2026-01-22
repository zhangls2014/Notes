plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.serialization)
}

android {
  namespace = "me.zhangls.network"
  compileSdk = libs.versions.compileSdk.get().toInt()
  buildToolsVersion = libs.versions.buildTool.get()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.extJunit)
  androidTestImplementation(libs.androidx.espresso)

  implementation(libs.androidx.core)
  implementation(libs.jetbrains.kotlinx.coroutines.android)
  api(libs.jetbrains.kotlinx.serialization.core)
  api(libs.jetbrains.kotlinx.serialization.json)
  api(libs.squareup.retrofit2)
  api(libs.squareup.retrofit2.kotlinx.serialization)
  api(libs.squareup.okhttp.logging)
}