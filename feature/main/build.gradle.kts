plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.serialization)
  alias(libs.plugins.jetbrains.compose.compiler)
  alias(libs.plugins.google.ksp)
}

android {
  namespace = "me.zhangls.main"
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
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.espresso)

  implementation(project(":core:data"))
  implementation(project(":core:theme"))
  implementation(project(":core:framework"))
  implementation(project(":feature:settings"))

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.activity)
  implementation(libs.androidx.compose.lifecycle)
  implementation(libs.androidx.compose.viewmodel)

  // DI
  implementation(libs.google.hilt.android)
  implementation(libs.androidx.hilt.navigation.compose)
  ksp(libs.google.hilt.android.compiler)

  // 自适应布局
  implementation(libs.androidx.compose.material3.window.size)
  implementation(libs.androidx.compose.material3.adaptive)
  implementation(libs.androidx.compose.material3.adaptive.layout)
  implementation(libs.androidx.compose.material3.adaptive.navigation)
  implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
}