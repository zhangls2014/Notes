plugins {
  id("me.zhangls.kmp-library")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(kmp.jetbrains.kotlinx.serialization.core)
      }
    }
  }
}
