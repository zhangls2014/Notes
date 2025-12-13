import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.kotlin)
  alias(libs.plugins.jetbrains.compose.compiler)
}

android {
  namespace = "me.zhangls.theme"
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

  buildFeatures {
    compose = true
  }
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_17
  }
}

dependencies {
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.extJunit)
  androidTestImplementation(libs.androidx.espresso)

  implementation(libs.androidx.core)
  implementation(libs.androidx.appcompat)
  implementation(libs.google.material)

  // Compose
  debugImplementation(libs.androidx.compose.uiTooling)
  debugImplementation(libs.androidx.compose.uiManifest)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.preview)
  implementation(libs.androidx.compose.constraintLayout)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.activity)
  implementation(libs.androidx.compose.lifecycle)
}