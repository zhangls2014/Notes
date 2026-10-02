plugins {
  id("me.zhangls.kmp-library")
  // 入口使用 Compose Runtime，结果契约由 framework 提供。
  id("me.zhangls.kmp-compose-api")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // SettingsResult 的父类型 MviEffect 出现在公开 API。
        api(projects.core.framework)
      }
    }
  }
}
