plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  id("me.zhangls.kmp-koin")
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(projects.core.theme)
      // aboutNavEntry exposes NavEffect and AboutAppInfo.
      api(projects.core.framework)
      api(projects.feature.aboutApi)
    }
    commonTest.dependencies { implementation(kmp.kotlin.test) }
  }
}
