import java.time.LocalDate
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.serialization)
  alias(libs.plugins.jetbrains.compose.compiler)
  alias(libs.plugins.google.ksp)
  alias(libs.plugins.google.hilt)
  alias(libs.plugins.androidx.baselineprofile)
}

val localProperties: Provider<Properties> = providers
  .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
  .asText
  .map { content ->
    val props = Properties()
    props.load(content.reader())
    props
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
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"

    ndk {
      abiFilters.add("arm64-v8a")
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      storeFile = file(path = localProperties.map { it.getProperty("signing.path") }.getOrElse(""))
      storePassword = localProperties.map { it.getProperty("signing.storePassword") }.getOrElse("")
      keyAlias = localProperties.map { it.getProperty("signing.keyAlias") }.getOrElse("")
      keyPassword = localProperties.map { it.getProperty("signing.keyPassword") }.getOrElse("")
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("release")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      signingConfig = signingConfigs.getByName("release")
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

baselineProfile {
  automaticGenerationDuringBuild = true
  saveInSrc = true
}

dependencies {
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.espresso)
  debugImplementation(libs.squareup.leakCanary)

  // Module
  implementation(project(":core:data"))
  implementation(project(":core:theme"))
  implementation(project(":core:network"))
  implementation(project(":core:framework"))
  implementation(project(":feature:main"))
  implementation(project(":feature:login"))
  implementation(project(":feature:settings"))

  // BaselineProfile
  baselineProfile(project(":baselineprofile"))
  implementation(libs.androidx.profileinstaller)

  // Core
  implementation(libs.androidx.core)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.appcompat)
  implementation(libs.google.material)

  // Compose
  debugImplementation(platform(libs.androidx.compose.bom))
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui.preview)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material3.window.size)
  implementation(libs.androidx.compose.activity)
  implementation(libs.androidx.compose.lifecycle)
  implementation(libs.androidx.compose.viewmodel)

  // DI
  implementation(libs.google.hilt.android)
  ksp(libs.google.hilt.android.compiler)
}
