plugins {
  alias(kmp.plugins.jetbrains.kotlin.serialization)
  alias(kmp.plugins.jetbrains.kotlin.multiplatform)
  alias(kmp.plugins.android.kmp.library)
  alias(kmp.plugins.android.lint)
  alias(kmp.plugins.google.ksp)
  alias(kmp.plugins.koin.compiler)
}

kotlin {
  android {
    namespace = "me.zhangls.framework"
    buildToolsVersion = kmp.versions.android.buildTools.get()

    compileSdk = kmp.versions.android.compileSdk.get().toInt()
    minSdk = kmp.versions.android.minSdk.get().toInt()
  }

  listOf(
    iosArm64(),
    iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "frameworkKit"
      isStatic = true
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(kmp.androidx.lifecycle.viewmodel.compose)
        api(kmp.androidx.lifecycle.runtime.compose)

        api(kmp.jetbrains.kotlinx.serialization.core)
        api(kmp.jetbrains.kotlinx.serialization.json)
        api(kmp.jetbrains.kotlinx.coroutines.core)
        api(kmp.jetbrains.kotlinx.collections.immutable)
        api(kmp.jetbrains.navigation3.ui)

        api(kmp.jetbrains.compose.components.resources)

        api(project.dependencies.platform(kmp.koin.bom))
        api(kmp.koin.core)
        api(kmp.koin.annotations)
        api(kmp.koin.compose)
        api(kmp.koin.compose.viewmodel)
      }
    }

    androidMain {
      dependencies {
        api(kmp.jetbrains.kotlinx.coroutines.android)
      }
    }
  }
}

dependencies {
  add("kspAndroid", kmp.androidx.room.compiler)
  add("kspIosArm64", kmp.androidx.room.compiler)
  add("kspIosSimulatorArm64", kmp.androidx.room.compiler)
}
