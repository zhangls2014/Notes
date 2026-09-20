import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.android.kotlin.multiplatform.library")
  id("me.zhangls.detekt")
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

    // 产出 androidHostTest 源集与 testAndroidHostTest 任务：让 commonTest 能在**宿主 JVM** 上跑。
    //
    // 为什么值得开：本项目的 commonTest 原本只有 iosSimulatorArm64Test 一条执行路径（分钟级 +
    // 需模拟器），反馈成本高到会直接抑制测试密度 —— 这是"测试写得少"的结构性原因，不是意识问题。
    // 开了之后全模块聚合 22s、单模块增量回环 4~5s（对照 iOS 侧约 2 分半）。
    //
    // 不新增 `jvm()` 目标来达到同样目的：KMP 里多一个 JVM 目标会改变依赖解析、
    // 可能引入重复的 actual 实现与发布配置，代价远大于收益。
    withHostTest {}
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

// 给 androidHostTest 变体的 lint 任务补上对 KSP 的依赖。
//
// AGP 为每个变体生成的 `generateAndroidHostTestLintModel` / `lintAnalyzeAndroidHostTest`
// 会读 `kspAndroidHostTest` 的产物（`build/generated/ksp/android/androidHostTest/`），
// 却**没有声明**这层依赖。于是这两个任务与测试出现在同一次 Gradle 调用里时
// （最典型的是 `check` —— 它同时带上测试与 lint），Gradle 9 的隐式依赖校验直接判构建失败：
//
//   Task ':core:data:generateAndroidHostTestLintModel' uses this output of task
//   ':core:data:kspAndroidHostTest' without declaring an explicit or implicit dependency
//
// 报错读起来像"配置写错了"，实际是 AGP 侧的缺声明。这里在约定插件里补齐，全模块生效。
//
// 用 dependsOn 而不是 mustRunAfter：后者只保证顺序、不保证产物已生成，
// 而我们要的是"别读到半成品"。外面包一层 matching，模块若没有 KSP 宿主测试任务则自然无操作。
tasks.matching {
  it.name == "generateAndroidHostTestLintModel" || it.name == "lintAnalyzeAndroidHostTest"
}.configureEach {
  dependsOn(tasks.matching { it.name == "kspAndroidHostTest" })
}
