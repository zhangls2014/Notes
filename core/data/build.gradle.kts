plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.serialization)
  alias(libs.plugins.google.ksp)
}

android {
  namespace = "me.zhangls.data"
  buildToolsVersion = libs.versions.buildTool.get()

  compileSdk {
    version = release(libs.versions.compileSdk.get().toInt())
  }

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
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.espresso)

  // DI
  implementation(libs.google.hilt.android)
  ksp(libs.google.hilt.android.compiler)

  // DataStore
  api(libs.androidx.datastore.preferences)

  // Json 序列化
  api(libs.jetbrains.kotlinx.serialization.core)
  api(libs.jetbrains.kotlinx.serialization.json)

  // database
  api(libs.androidx.room.runtime)
  api(libs.androidx.room.ktx)
  api(libs.androidx.room.paging)
  ksp(libs.androidx.room.compiler)
}