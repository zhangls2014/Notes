import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
  alias(kmp.plugins.android.application)
  alias(kmp.plugins.androidx.baselineprofile)
  alias(kmp.plugins.jetbrains.kotlin.compose.compiler)
  alias(kmp.plugins.jetbrains.compose)
}

kotlin {
  target {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_21
    }
  }

  dependencies {
    testImplementation(kmp.junit)
    androidTestImplementation(kmp.androidx.test.ext.junit)
    androidTestImplementation(kmp.androidx.test.espresso)
    debugImplementation(kmp.squareup.leakCanary)

    // modules
    implementation(projects.core.data)
    implementation(projects.composeApp)

    // libraries
    implementation(kmp.androidx.activity.compose)
    implementation(kmp.jetbrains.compose.ui.tooling.preview)
    implementation(kmp.jetbrains.compose.material3)

    // BaselineProfile
    //baselineProfile(projects.android.baselineprofile)
    implementation(kmp.androidx.profileinstaller)

    // Core
    implementation(kmp.androidx.core)
    implementation(kmp.androidx.appcompat)
  }
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
  buildToolsVersion = kmp.versions.android.buildTools.get()

  compileSdk {
    version = release(kmp.versions.android.compileSdk.get().toInt())
  }

  defaultConfig {
    applicationId = "me.zhangls.notes"
    minSdk = kmp.versions.android.minSdk.get().toInt()
    targetSdk = kmp.versions.android.targetSdk.get().toInt()
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
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  buildFeatures {
    compose = true
  }
}

baselineProfile {
  automaticGenerationDuringBuild = false
  saveInSrc = true
}
