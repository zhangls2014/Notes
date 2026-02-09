plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.compose.compiler)
}

android {
  namespace = "me.zhangls.theme"
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

  buildFeatures {
    compose = true
  }
}

dependencies {
  testImplementation(libs.junit)
  androidTestImplementation(libs.bundles.androidx.test)

  implementation(libs.androidx.core)
  implementation(libs.androidx.appcompat)
  implementation(libs.google.material)

  // Compose
  val composeBom = platform(libs.androidx.compose.bom)
  debugImplementation(composeBom)
  debugImplementation(libs.bundles.androidx.compose.test)
  implementation(composeBom)
  implementation(libs.bundles.androidx.compose.basic)
}