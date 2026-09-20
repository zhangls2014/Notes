import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

// 本插件由模块与 me.zhangls.kmp-library 组合应用（library 先应用，Kotlin 扩展已存在）
val kmp = extensions.getByType<VersionCatalogsExtension>().named("kmp")

// 与 me.zhangls.kmp-compose 的唯一区别：只注入 compose-runtime。
//
// feature 的 -api 契约里只有 @Composable 入口，既不需要 foundation / material3，
// 也不需要 compose-resources；而 kmp-compose 会把这六项一并注入，并顺传递扩散到
// 所有依赖该契约的模块。契约模块的依赖应当尽量薄 —— 依赖越少，越不容易在无意间
// 破坏 api / impl 的可见性边界。
configure<KotlinMultiplatformExtension> {
  sourceSets {
    commonMain {
      dependencies {
        implementation(kmp.findLibrary("jetbrains-compose-runtime").get())
      }
    }
  }
}
