plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-koin")
  alias(kmp.plugins.android.lint)
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      api(project.dependencies.platform(kmp.ktor.bom))
      api(kmp.ktor.client.core)
      implementation(kmp.ktor.client.cio)
      implementation(kmp.ktor.client.auth)
      implementation(kmp.ktor.client.logging)
      implementation(kmp.ktor.client.content.negotiation)
      implementation(kmp.ktor.serialization.kotlinx.json)

      implementation(kmp.jetbrains.kotlinx.coroutines.core)

      implementation(kmp.jetbrains.kotlinx.serialization.core)
      implementation(kmp.jetbrains.kotlinx.serialization.json)
    }
    androidMain.dependencies {
      implementation(kmp.ktor.client.okhttp)
      implementation(kmp.jetbrains.kotlinx.coroutines.android)
    }
    iosMain.dependencies {
      implementation(kmp.ktor.client.darwin)
    }
  }
}

koinCompiler {
  // TODO 0.4.1 版本，@Provided 注解无效，导致编译失败。所以暂时先禁用
  compileSafety = false
}
