import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(kmp.plugins.jetbrains.kotlin.serialization)
  alias(kmp.plugins.jetbrains.kotlin.multiplatform)
  alias(kmp.plugins.jetbrains.kotlin.compose.compiler)
  alias(kmp.plugins.jetbrains.compose)
  alias(kmp.plugins.android.kmp.library)
  alias(kmp.plugins.google.ksp)
  alias(kmp.plugins.koin.compiler)
}

kotlin {
  android {
    namespace = "me.zhangls.login"
    buildToolsVersion = kmp.versions.android.buildTools.get()

    compileSdk = kmp.versions.android.compileSdk.get().toInt()
    minSdk = kmp.versions.android.minSdk.get().toInt()

    compilerOptions {
      jvmTarget = JvmTarget.JVM_21
    }

    androidResources {
      enable = true
    }
  }

  listOf(
    iosArm64(),
    iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "loginKit"
      isStatic = true
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core.data)
        implementation(projects.core.theme)
        implementation(projects.core.framework)
        implementation(projects.feature.settings)

        implementation(kmp.jetbrains.compose.runtime)
        implementation(kmp.jetbrains.compose.foundation)
        implementation(kmp.jetbrains.compose.ui)
        implementation(kmp.jetbrains.compose.ui.tooling.preview)
        implementation(kmp.jetbrains.compose.components.resources)
        implementation(kmp.jetbrains.compose.material3)
      }
    }
  }
}
