plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.kotlin.serialization)
  alias(libs.plugins.google.ksp)
  alias(libs.plugins.koin.compiler)
}

android {
  namespace = "me.zhangls.network"
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

koinCompiler {
  // 0.4.1 版本，@Provided 注解无效，导致编译失败。所以暂时先禁用
  compileSafety = false
}

dependencies {
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.espresso)

  implementation(libs.androidx.core)
  implementation(libs.jetbrains.kotlinx.coroutines.android)
  api(libs.jetbrains.kotlinx.serialization.core)
  api(libs.jetbrains.kotlinx.serialization.json)
  api(libs.squareup.retrofit2)
  api(libs.squareup.retrofit2.kotlinx.serialization)
  api(libs.squareup.okhttp.logging)

  // DI
  api(platform(libs.koin.bom))
  api(libs.koin.core)
  api(libs.koin.annotations)
}