import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.api.variant.impl.capitalizeFirstChar
import java.util.Properties

plugins {
  alias(kmp.plugins.android.application)
  alias(kmp.plugins.androidx.baselineprofile)
  alias(kmp.plugins.jetbrains.kotlin.compose.compiler)
  alias(kmp.plugins.jetbrains.compose)
}

kotlin {
  // 显式声明编译用 JDK：否则回退到运行 Gradle 的 JDK，
  // 而 IDE（Android Studio 自带 JBR）与命令行的默认 JDK 可能不同
  jvmToolchain(kmp.versions.jvm.target.get().toInt())

  dependencies {
    testImplementation(kmp.junit)
    androidTestImplementation(kmp.androidx.test.ext.junit)
    androidTestImplementation(kmp.androidx.test.espresso)
    debugImplementation(kmp.squareup.leakCanary)

    // modules
    implementation(projects.core.data)
    // LocaleUtils 直接用 AppLanguage，显式声明而不是依赖 core:data 的 api 传递
    implementation(projects.core.model)
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

    // Koin
    implementation(platform(kmp.koin.bom))
    implementation(kmp.koin.android)
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
      // 仅在 local.properties 提供签名信息时配置，避免无密钥环境无法构建
      val storeFilePath = localProperties.map { it.getProperty("signing.path") }.getOrElse("")
      if (storeFilePath.isNotEmpty()) {
        storeFile = file(storeFilePath)
        storePassword = localProperties.map { it.getProperty("signing.storePassword") }.getOrElse("")
        keyAlias = localProperties.map { it.getProperty("signing.keyAlias") }.getOrElse("")
        keyPassword = localProperties.map { it.getProperty("signing.keyPassword") }.getOrElse("")
      }
    }
  }

  buildTypes {
    debug {
      // debug 构建使用默认 debug 签名，不挂 release 密钥与混淆配置
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
  flavorDimensions.add("language")
  productFlavors {
    create("dev") {
      dimension = "environment"
    }

    create("prd") {
      dimension = "environment"
    }

    create("mainland") {
      dimension = "language"
    }

    create("full") {
      dimension = "language"
    }
  }
}

androidComponents {
  onVariants { variant ->
    variant.outputs.forEach { output ->
      if (output is VariantOutputImpl) {
        output.outputFileName = "Notes_v${output.versionName.get()}(${output.versionCode.get()})" +
            "_${variant.flavorName?.capitalizeFirstChar()}_${variant.buildType}.apk"
      }
    }
  }
}

baselineProfile {
  automaticGenerationDuringBuild = false
  saveInSrc = true
}
