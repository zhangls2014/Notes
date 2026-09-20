import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.android.kotlin.multiplatform.library")
}

// 预编译脚本插件无版本目录访问器，显式获取 "kmp" 目录
val kmp = extensions.getByType<VersionCatalogsExtension>().named("kmp")

// JVM 编译级别由版本目录统一供给，不在各模块硬编码
val jvmTargetVersion = kmp.findVersion("jvm-target").get().requiredVersion

kotlin {
  // 显式声明编译用 JDK：否则回退到运行 Gradle 的 JDK，
  // 而 IDE（Android Studio 自带 JBR）与命令行的默认 JDK 可能不同
  jvmToolchain(jvmTargetVersion.toInt())

  android {
    namespace = me.zhangls.convention.deriveNamespace(project.path)
    buildToolsVersion = kmp.findVersion("android-buildTools").get().requiredVersion

    compileSdk = kmp.findVersion("android-compileSdk").get().requiredVersion.toInt()
    minSdk = kmp.findVersion("android-minSdk").get().requiredVersion.toInt()

    compilerOptions {
      jvmTarget = JvmTarget.fromTarget(jvmTargetVersion)
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
