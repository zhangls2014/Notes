plugins {
  id("me.zhangls.kmp-library")
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      api(project.dependencies.platform(kmp.ktor.bom))
      api(kmp.ktor.client.core)
      implementation(kmp.jetbrains.kotlinx.coroutines.core)
      implementation(kmp.jetbrains.kotlinx.serialization.json)
    }
    commonTest.dependencies {
      implementation(kmp.kotlin.test)
      implementation(kmp.jetbrains.kotlinx.coroutines.test)
    }
  }
}
