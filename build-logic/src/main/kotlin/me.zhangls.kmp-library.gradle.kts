import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.android.kotlin.multiplatform.library")
}

// 预编译脚本插件无版本目录访问器，显式获取 "kmp" 目录
val kmp = extensions.getByType<VersionCatalogsExtension>().named("kmp")

kotlin {
  android {
    namespace = me.zhangls.convention.deriveNamespace(project.path)
    buildToolsVersion = kmp.findVersion("android-buildTools").get().requiredVersion

    compileSdk = kmp.findVersion("android-compileSdk").get().requiredVersion.toInt()
    minSdk = kmp.findVersion("android-minSdk").get().requiredVersion.toInt()

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
      baseName = me.zhangls.convention.deriveFrameworkName(project.path)
      isStatic = true
    }
  }
}
