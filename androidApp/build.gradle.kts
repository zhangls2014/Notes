import java.net.URI
import me.zhangls.convention.localBuildProperties
import me.zhangls.convention.notesServiceProperty
import me.zhangls.convention.requiredSetting
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(kmp.plugins.android.application)
  alias(kmp.plugins.androidx.baselineprofile)
  alias(kmp.plugins.jetbrains.kotlin.compose.compiler)
  alias(kmp.plugins.jetbrains.compose)
  // 本模块不适用 me.zhangls.kmp-library，故单独引入静态分析约定
  id("me.zhangls.detekt")
}

kotlin {
  // 显式声明编译用 JDK：否则回退到运行 Gradle 的 JDK，
  // 而 IDE（Android Studio 自带 JBR）与命令行的默认 JDK 可能不同
  jvmToolchain(kmp.versions.jdk.get().toInt())
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(kmp.versions.jvm.target.get())
  }

  dependencies {
    testImplementation(kmp.junit)
    testImplementation(kmp.jetbrains.kotlinx.coroutines.test)
    androidTestImplementation(kmp.androidx.test.ext.junit)
    androidTestImplementation(kmp.androidx.test.espresso)
    debugImplementation(kmp.squareup.leakCanary)

    // modules
    implementation(projects.core.data)
    // LocaleUtils 直接用 AppLanguage，显式声明而不是依赖 core:data 的 api 传递
    implementation(projects.core.model)
    implementation(projects.composeApp)
    implementation(projects.feature.loginApi)

    // libraries
    implementation(kmp.androidx.activity.compose)
    implementation(kmp.androidx.biometric)
    implementation(kmp.androidx.credentials)
    implementation(kmp.jetbrains.compose.ui.tooling.preview)
    implementation(kmp.jetbrains.compose.material3)
    implementation(kmp.materialkolor.material3)

    // BaselineProfile
    //baselineProfile(projects.android.baselineprofile)
    implementation(kmp.androidx.profileinstaller)

    // Core
    implementation(kmp.androidx.core)
    implementation(kmp.androidx.appcompat)

    // Koin
    implementation(platform(kmp.koin.bom))
    implementation(kmp.koin.android)
  }
}

val localSigning = localBuildProperties("signing.properties")
val localDirectory = rootProject.layout.projectDirectory.dir(".local")

fun signingProperty(
  gradleProperty: String,
  environmentVariable: String,
): String = providers
  .gradleProperty(gradleProperty)
  .orElse(providers.environmentVariable(environmentVariable))
  .orElse(localSigning.map { settings ->
    settings.getProperty(gradleProperty)?.let { value ->
      if (gradleProperty == "notes.signing.path") localDirectory.file(value).asFile.absolutePath else value
    }
  })
  .requiredSetting(gradleProperty, "signing.properties")

val signingPath = signingProperty("notes.signing.path", "NOTES_SIGNING_PATH")
val signingStorePassword = signingProperty(
  "notes.signing.storePassword",
  "NOTES_SIGNING_STORE_PASSWORD",
)
val signingKeyAlias = signingProperty("notes.signing.keyAlias", "NOTES_SIGNING_KEY_ALIAS")
val signingKeyPassword = signingProperty(
  "notes.signing.keyPassword",
  "NOTES_SIGNING_KEY_PASSWORD",
)

val notesSiteOrigin = notesServiceProperty("notes.siteBaseUrl", "NOTES_SITE_BASE_URL")
  .requiredSetting("notes.siteBaseUrl", "server.properties")
require(Regex("https://[A-Za-z0-9.-]+/?").matches(notesSiteOrigin)) { "Invalid Notes website origin" }
val notesSiteHost = requireNotNull(URI.create(notesSiteOrigin).host) { "Invalid Notes website host" }.lowercase()

android {
  namespace = "me.zhangls.notes"
  buildToolsVersion = kmp.versions.android.buildTools.get()

  compileSdk {
    version = release(kmp.versions.android.compileSdk.get().toInt()) {
      minorApiLevel = kmp.versions.android.compileSdkMinor.get().toInt()
    }
  }

  defaultConfig {
    applicationId = "me.zhangls.notes"
    manifestPlaceholders["notesSiteHost"] = notesSiteHost
    minSdk = kmp.versions.android.minSdk.get().toInt()
    targetSdk = kmp.versions.android.targetSdk.get().toInt()
    versionCode = 8
    versionName = "1.1.5"

    ndk {
      abiFilters.add("arm64-v8a")
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      storeFile = file(signingPath)
      storePassword = signingStorePassword
      keyAlias = signingKeyAlias
      keyPassword = signingKeyPassword
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("release")
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      signingConfig = signingConfigs.getByName("release")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(kmp.versions.jvm.target.get())
    targetCompatibility = JavaVersion.toVersion(kmp.versions.jvm.target.get())
  }

  buildFeatures {
    compose = true
  }

  flavorDimensions.add("environment")
  flavorDimensions.add("region")
  productFlavors {
    create("dev") {
      dimension = "environment"
    }

    create("prd") {
      dimension = "environment"
    }

    create("mainland") {
      dimension = "region"
      manifestPlaceholders["notesUpdateChannel"] = "mainland"
    }

    create("global") {
      dimension = "region"
      manifestPlaceholders["notesUpdateChannel"] = "global"
    }
  }
}

androidComponents {
  onVariants { variant ->
    val flavorLabel = variant.flavorName?.replaceFirstChar { it.titlecase() }
    variant.outputs.forEach { output ->
      output.outputFileName.set(
        output.versionName.zip(output.versionCode) { name, code ->
          "Notes_v${name}(${code})_${flavorLabel}_${variant.buildType}.apk"
        }
      )
    }
  }
}

baselineProfile {
  automaticGenerationDuringBuild = false
  saveInSrc = true
}
