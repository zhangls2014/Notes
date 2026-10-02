plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose-api")
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      // Destination、RequireLogin 和 NavigationContribution 出现在公开 API。
      api(projects.core.framework)
    }
  }
}
