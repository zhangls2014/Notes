plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  alias(kmp.plugins.android.lint)
}

kotlin {
  android {
    compilations.withType<com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation>().configureEach {
      isIncludeAndroidResources = true
    }
  }
  sourceSets {
    commonMain.dependencies {
      // NavEntry and SceneStrategy are part of the public display and safe-area APIs.
      api(kmp.jetbrains.navigation3.ui)
      api(kmp.jetbrains.kotlinx.collections.immutable)
      api(kmp.jetbrains.compose.runtime)
      implementation(kmp.androidx.navigationevent.compose)
    }
    androidMain.dependencies {
      implementation(kmp.androidx.core)
    }
    androidHostTest.dependencies {
      implementation(kmp.junit)
      implementation(kmp.androidx.activity.compose)
      implementation(kmp.androidx.test.espresso)
      implementation(kmp.androidx.test.ext.junit)
      implementation(kmp.robolectric)
      implementation(kmp.jetbrains.compose.ui.test.junit4)
    }
  }
}

// Native Graphics tests use the same runtime conditions as the application host tests.
tasks.withType<Test>().configureEach {
  maxHeapSize = "4g"
  jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
  maxParallelForks = 1
}
