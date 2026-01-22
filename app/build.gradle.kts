plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.serialization)
  alias(libs.plugins.jetbrains.compose.compiler)
  alias(libs.plugins.google.ksp)
  alias(libs.plugins.google.hilt)
}

android {
  namespace = "me.zhangls.notes"
  compileSdk = libs.versions.compileSdk.get().toInt()
  buildToolsVersion = libs.versions.buildTool.get()

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
  androidTestImplementation(libs.androidx.extJunit)
  androidTestImplementation(libs.androidx.espresso)
  debugImplementation(libs.squareup.leakCanary)
  // Module
  implementation(project(":theme"))
  implementation(project(":network"))
  // Core
  implementation(libs.androidx.core)
  implementation(libs.androidx.appcompat)
  implementation(libs.google.material)
  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.preview)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.activity)
  implementation(libs.androidx.compose.lifecycle)
  implementation(libs.androidx.compose.viewmodel)
  // DI
  implementation(libs.google.hilt.android)
  ksp(libs.google.hilt.android.compiler)
  // DataStore
  implementation(libs.androidx.datastore.preferences)
  // Navigation3
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.compose.material.adapter.navigation)
  implementation(libs.androidx.compose.viewmodel.navigation3)
}