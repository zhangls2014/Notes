import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(kmp.plugins.jetbrains.kotlin.multiplatform)
  alias(kmp.plugins.jetbrains.kotlin.compose.compiler)
  alias(kmp.plugins.jetbrains.compose)
  alias(kmp.plugins.android.kmp.library)
}

kotlin {
  android {
    namespace = "me.zhangls.settings.api"
    buildToolsVersion = kmp.versions.android.buildTools.get()

    compileSdk = kmp.versions.android.compileSdk.get().toInt()
    minSdk = kmp.versions.android.minSdk.get().toInt()

    compilerOptions {
      jvmTarget = JvmTarget.JVM_21
    }
  }

  listOf(
    iosArm64(),
    iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "settingsApiKit"
      isStatic = true
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        // 契约依赖 framework 中的 MviEffect
        implementation(projects.core.framework)

        // Compose（接口中的 @Composable 入口）
        implementation(kmp.jetbrains.compose.runtime)
      }
    }
  }
}
