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
    namespace = "me.zhangls.network"
    buildToolsVersion = kmp.versions.android.buildTools.get()

    compileSdk = kmp.versions.android.compileSdk.get().toInt()
    minSdk = kmp.versions.android.minSdk.get().toInt()
  }

  listOf(
    iosArm64(),
    iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "networkKit"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(project.dependencies.platform(kmp.ktor.bom))
      implementation(kmp.ktor.client.core)
      implementation(kmp.ktor.client.cio)
      implementation(kmp.ktor.client.auth)
      implementation(kmp.ktor.client.logging)
      implementation(kmp.ktor.client.content.negotiation)
      implementation(kmp.ktor.serialization.kotlinx.json)

      implementation(kmp.jetbrains.kotlinx.coroutines.core)
      implementation(kmp.jetbrains.kotlinx.serialization.core)
      implementation(kmp.jetbrains.kotlinx.serialization.json)

      // DI
      implementation(project.dependencies.platform(kmp.koin.bom))
      implementation(kmp.koin.core)
      implementation(kmp.koin.annotations)
    }
    androidMain.dependencies {
      implementation(kmp.ktor.client.okhttp)
      implementation(kmp.jetbrains.kotlinx.coroutines.android)
    }
    iosMain.dependencies {
      implementation(kmp.ktor.client.darwin)
    }
  }
}

koinCompiler {
  // TODO 0.4.1 版本，@Provided 注解无效，导致编译失败。所以暂时先禁用
  compileSafety = false
}