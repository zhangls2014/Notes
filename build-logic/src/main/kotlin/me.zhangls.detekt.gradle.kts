import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
  id("dev.detekt")
}

// 预编译脚本插件无版本目录访问器，显式获取 "kmp" 目录
val kmp = extensions.getByType<VersionCatalogsExtension>().named("kmp")

// 本插件会被应用到各子模块，配置路径必须锚定根项目：
// 直接用 file(...) 会按子模块目录解析，找不到根目录下的配置
val detektConfigFile = rootProject.layout.projectDirectory.file("config/detekt/detekt.yml")

detekt {
  toolVersion = kmp.findVersion("detekt").get().requiredVersion
  config.setFrom(detektConfigFile)
  buildUponDefaultConfig = true
  // 2.x 移除了 build.maxIssues（原值为 0，即"任何 finding 都失败"）。
  // 这里用 Info 复刻该零容忍语义：Info / Warning / Error 任一等级都算失败。
  failOnSeverity = FailOnSeverity.Info
  // baseline：存量问题豁免清单（由 ./gradlew <module>:detektBaseline 生成）。
  // 2.x 的 baseline 路径没有默认值，不显式指定则不启用。
  // 每模块一份（<module>/config/detekt/baseline.xml），避免并行任务共写同一根文件互相覆盖。
  baseline.set(project.layout.projectDirectory.file("config/detekt/baseline.xml"))
}

tasks.withType<Detekt>().configureEach {
  reports {
    html.required.set(true)
    markdown.required.set(true)
  }
}

dependencies {
  detektPlugins(kmp.findLibrary("detekt-formatting").get())
}

// detekt 的默认源目录是 JVM 布局（src/main/kotlin、src/test/kotlin），
// 而 KMP 模块的代码在 src/commonMain/kotlin、src/androidMain/kotlin、src/iosMain/kotlin……
// 不显式喂进去，detekt 任务恒为 NO-SOURCE（本仓库已实测确认）。
// 注意 DetektCreateBaselineTask 直接继承 SourceTask、不是 Detekt 子类型，
// 两类任务都要喂，否则 detektBaseline 生成空 baseline。
// 非 KMP 模块（androidApp / baselineprofile）走标准 JVM 布局，无需干预。
pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
  val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
  val moduleDir = projectDir.invariantSeparatorsPath

  val kmpSourceDirs = kotlin.sourceSets
    .flatMap { it.kotlin.srcDirs }
    .filter { dir ->
      // 排除 KSP / BuildKonfig 等生成目录，只扫手写源码
      dir.exists() && !dir.invariantSeparatorsPath.removePrefix(moduleDir).startsWith("/build/")
    }

  tasks.withType<Detekt>().configureEach { setSource(kmpSourceDirs) }
  tasks.withType<DetektCreateBaselineTask>().configureEach { setSource(kmpSourceDirs) }
}
