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
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.espresso)
  debugImplementation(libs.squareup.leakCanary)

  // Module
  implementation(project(":theme"))
  implementation(project(":network"))
  implementation(project(":framework"))
  implementation(project(":data"))
  implementation(project(":login"))

  // Core
  implementation(libs.androidx.core)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.appcompat)
  implementation(libs.google.material)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui.preview)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.activity)
  implementation(libs.androidx.compose.lifecycle)
  implementation(libs.androidx.compose.viewmodel)

  // DI
  implementation(libs.google.hilt.android)
  ksp(libs.google.hilt.android.compiler)
}