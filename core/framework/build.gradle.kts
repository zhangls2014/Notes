plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-koin")
  alias(kmp.plugins.android.lint)
}

kotlin {
  sourceSets {
    commonTest.dependencies {
      implementation(kmp.kotlin.test)
    }
    commonMain {
      dependencies {
        api(kmp.androidx.lifecycle.viewmodel.compose)
        api(kmp.androidx.lifecycle.runtime.compose)

        api(kmp.jetbrains.kotlinx.serialization.core)
        api(kmp.jetbrains.kotlinx.serialization.json)
        api(kmp.jetbrains.kotlinx.coroutines.core)
        api(kmp.jetbrains.kotlinx.collections.immutable)
        api(kmp.jetbrains.navigation3.ui)

        api(kmp.jetbrains.compose.components.resources)

        api(kmp.koin.core)
        api(kmp.koin.compose)
        api(kmp.koin.compose.viewmodel)
      }
    }

    androidMain {
      dependencies {
        api(kmp.jetbrains.kotlinx.coroutines.android)
      }
    }
  }
}
