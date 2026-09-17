import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

// 本插件由模块与 me.zhangls.kmp-library 组合应用（library 先应用，Kotlin 扩展已存在）
val kmp = extensions.getByType<VersionCatalogsExtension>().named("kmp")

configure<KotlinMultiplatformExtension> {
  sourceSets {
    commonMain {
      dependencies {
        implementation(kmp.findLibrary("jetbrains-compose-runtime").get())
        implementation(kmp.findLibrary("jetbrains-compose-foundation").get())
        implementation(kmp.findLibrary("jetbrains-compose-ui").get())
        implementation(kmp.findLibrary("jetbrains-compose-ui-tooling-preview").get())
        implementation(kmp.findLibrary("jetbrains-compose-components-resources").get())
        implementation(kmp.findLibrary("jetbrains-compose-material3").get())
      }
    }
  }
}
