import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(kmp.plugins.jetbrains.kotlin.serialization)
  alias(kmp.plugins.jetbrains.kotlin.multiplatform)
  alias(kmp.plugins.jetbrains.kotlin.compose.compiler)
  alias(kmp.plugins.jetbrains.compose)
  alias(kmp.plugins.android.kmp.library)
  alias(kmp.plugins.google.ksp)
  alias(kmp.plugins.koin.compiler)
  alias(kmp.plugins.buildKonfig)
}

kotlin {
  android {
    namespace = "me.zhangls.entry"
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
      baseName = "ComposeApp"
      isStatic = true
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        // Module
        implementation(projects.core.data)
        implementation(projects.core.theme)
        implementation(projects.core.network)
        implementation(projects.core.framework)
        implementation(projects.feature.email)
        implementation(projects.feature.main)
        implementation(projects.feature.login)
        implementation(projects.feature.settings)

        // Compose
        implementation(kmp.jetbrains.compose.material3)
      }
    }

    commonTest.dependencies {
      implementation(kmp.kotlin.test)
    }

    androidMain.dependencies {
      // Core
      implementation(kmp.androidx.activity.compose)
      implementation(kmp.androidx.lifecycle.compose)
      implementation(kmp.androidx.viewmodel.compose)
      implementation(kmp.androidx.viewmodel.navigation3)
    }
  }
}

koinCompiler {
  userLogs = true
  compileSafety = false
  unsafeDslChecks = false
}

/**
 * 编译期常量配置
 */
buildkonfig {
  packageName = "me.zhangls.entry"
  defaultConfigs {
    buildConfigField(STRING, "BASE_URL", "https://www.mxnzp.com/")
  }
}
