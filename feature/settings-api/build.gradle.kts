plugins {
  id("me.zhangls.kmp-library")
  // 契约里只有 @Composable 入口，故只注入 compose-runtime（见约定插件）
  id("me.zhangls.kmp-compose-api")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // 契约依赖 framework 中的 MviEffect
        implementation(projects.core.framework)
      }
    }
  }
}
