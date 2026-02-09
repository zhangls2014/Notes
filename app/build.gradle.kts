plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.serialization)
  alias(libs.plugins.jetbrains.compose.compiler)
  alias(libs.plugins.google.ksp)
  alias(libs.plugins.google.hilt)
}

android {
  namespace = "me.zhangls.notes"
  buildToolsVersion = libs.versions.buildTool.get()

  compileSdk {
    version = release(libs.versions.compileSdk.get().toInt())
  }

  defaultConfig {
    applicationId = "me.zhangls.notes"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  val flavorEnv = "environment"
  flavorDimensions.add(flavorEnv)

  productFlavors {
    create("dev") {
      dimension = flavorEnv
      buildConfigField("String", "BASE_URL", "\"https://www.mxnzp.com/\"")
    }
  }
}

dependencies {
  testImplementation(libs.junit)
  androidTestImplementation(libs.bundles.androidx.test)
  debugImplementation(libs.squareup.leakCanary)
  // Module
  implementation(project(":theme"))
  implementation(project(":network"))
  implementation(project(":framework"))
  // Core
  implementation(libs.androidx.core)
  implementation(libs.androidx.appcompat)
  implementation(libs.google.material)
  // Compose
  val composeBom = platform(libs.androidx.compose.bom)
  debugImplementation(composeBom)
  debugImplementation(libs.bundles.androidx.compose.test)
  implementation(composeBom)
  implementation(libs.bundles.androidx.compose.basic)

  // DI
  implementation(libs.google.hilt.android)
  ksp(libs.google.hilt.android.compiler)
  // DataStore
  implementation(libs.androidx.datastore.preferences)
  // Navigation3
  implementation(libs.bundles.androidx.compose.navigation3)
}