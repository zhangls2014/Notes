plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose-api")
}

kotlin {
  sourceSets.commonMain.dependencies {
    // Destination and NavigationContribution appear in public contracts.
    api(projects.core.framework)
  }
}
