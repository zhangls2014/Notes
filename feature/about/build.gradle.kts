plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  id("me.zhangls.kmp-koin")
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(projects.core.theme)
      implementation(projects.core.network)
      implementation(kmp.jetbrains.kotlinx.serialization.json)
      implementation(kmp.androidx.navigationevent.compose)
      implementation(kmp.jetbrains.compose.material3.adaptive)
      implementation(kmp.jetbrains.compose.material3.adaptive.layout)
      implementation(kmp.jetbrains.compose.material3.adaptive.navigation3)
      // aboutNavEntry exposes NavEffect and AboutAppInfo.
      api(projects.core.framework)
      api(projects.feature.aboutApi)
    }
    androidMain.dependencies {
      implementation(kmp.androidx.core)
    }
    commonTest.dependencies {
      implementation(kmp.kotlin.test)
      implementation(kmp.jetbrains.kotlinx.coroutines.test)
    }
  }
}
