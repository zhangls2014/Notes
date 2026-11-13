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

      implementation(kmp.jetbrains.kotlinx.coroutines.core)
    }
    commonTest.dependencies {
      implementation(kmp.kotlin.test)
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
  compileSafety = true
}
