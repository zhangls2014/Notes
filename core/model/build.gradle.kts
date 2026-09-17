import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(kmp.plugins.jetbrains.kotlin.serialization)
  alias(kmp.plugins.jetbrains.kotlin.multiplatform)
  alias(kmp.plugins.android.kmp.library)
}

kotlin {
  android {
    namespace = "me.zhangls.model"
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
      baseName = "modelKit"
      isStatic = true
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(kmp.jetbrains.kotlinx.serialization.core)
      }
    }
  }
}
