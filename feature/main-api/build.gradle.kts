plugins {
  id("me.zhangls.kmp-library")
  // 契约里只有 @Composable 入口与导航 key，故只注入 compose-runtime（见约定插件）
  id("me.zhangls.kmp-compose-api")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // api 而非 implementation：Destination / DeepLinkDestination / RequireLogin 是
        // TabDestination 的父类型，属于本模块公开 API 的一部分，消费方必须能看到它们
        api(projects.core.framework)
      }
    }
  }
}
