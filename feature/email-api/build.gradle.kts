plugins {
  id("me.zhangls.kmp-library")
  alias(kmp.plugins.jetbrains.compose)
  alias(kmp.plugins.jetbrains.kotlin.compose.compiler)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // Compose（接口中的 @Composable 入口）
        implementation(kmp.jetbrains.compose.runtime)

      }
    }
  }
}
