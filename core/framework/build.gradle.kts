plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "me.zhangls.framework"
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

  implementation(libs.androidx.core)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.savedstate)
  implementation(libs.jetbrains.kotlinx.coroutines.android)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.runtime)

  // Navigation3
  api(libs.androidx.hilt.navigation.compose)
  api(libs.androidx.navigation3.runtime)
  api(libs.androidx.navigation3.ui)
  api(libs.androidx.compose.viewmodel.navigation3)
}