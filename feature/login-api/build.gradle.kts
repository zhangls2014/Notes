plugins {
  id("me.zhangls.kmp-library")
  // 契约里只有 @Composable 入口与导航 key，故只注入 compose-runtime（见约定插件）
  id("me.zhangls.kmp-compose-api")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // api：MviEffect（LoginResult 的父类型）与 Destination（LoginDestination 的父类型）
        // 都出现在本模块的公开 API 里
        api(projects.core.framework)
      }
    }
  }
}
