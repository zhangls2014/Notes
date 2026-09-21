plugins {
  id("me.zhangls.kmp-library")
  // 契约里只有 @Composable 入口与导航 key，故只注入 compose-runtime（见约定插件）
  id("me.zhangls.kmp-compose-api")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // api 而非 implementation：Destination / DeepLinkDestination / RequireLogin 是本模块
        // 导航 key 的父类型，NavigationContribution 的 SerializersModule 也来自 framework，
        // 都属于公开 API 的一部分，消费方必须能看到
        api(projects.core.framework)
      }
    }
  }
}
