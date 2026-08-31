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
    namespace = "me.zhangls.main"
    buildToolsVersion = kmp.versions.android.buildTools.get()

    compileSdk = kmp.versions.android.compileSdk.get().toInt()
    minSdk = kmp.versions.android.minSdk.get().toInt()

    compilerOptions {
      jvmTarget = JvmTarget.JVM_25
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
      baseName = "mainKit"
      isStatic = true
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core.data)
        implementation(projects.core.theme)
        implementation(projects.core.network)
        implementation(projects.core.framework)

        // 依赖 email 契约(api)；HomeScreen / FavoritesScreen 由 Koin 提供 EmailEntry 实现
        implementation(projects.feature.emailApi)

        // 依赖 main 契约(api)；实现类 MainEntryImpl 通过 Koin 绑定到 MainEntry
        implementation(projects.feature.mainApi)

        implementation(projects.feature.settingsApi)

        implementation(kmp.jetbrains.compose.runtime)
        implementation(kmp.jetbrains.compose.foundation)
        implementation(kmp.jetbrains.compose.ui)
        implementation(kmp.jetbrains.compose.ui.tooling.preview)
        implementation(kmp.jetbrains.compose.components.resources)
        implementation(kmp.jetbrains.compose.material3)

        // 自适应布局
        implementation(kmp.jetbrains.compose.material3.window.size)
        implementation(kmp.jetbrains.compose.material3.adaptive)
        implementation(kmp.jetbrains.compose.material3.adaptive.layout)
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation)
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation.suite)
      }
    }
  }
}
