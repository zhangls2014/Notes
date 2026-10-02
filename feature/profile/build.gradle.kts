plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  id("me.zhangls.kmp-koin")
}

kotlin {
  android {
    compilations.withType<com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation>().configureEach {
      isIncludeAndroidResources = true
    }
  }
  sourceSets {
    androidHostTest.dependencies {
      implementation(kmp.junit)
      implementation(kmp.androidx.test.espresso)
      implementation(kmp.androidx.test.ext.junit)
      implementation(kmp.robolectric)
      implementation(kmp.jetbrains.compose.ui.test.junit4)
      implementation(kmp.jetbrains.kotlinx.coroutines.test)
    }
    commonMain.dependencies {
      implementation(projects.core.data)
      implementation(projects.core.theme)
      // NavEffect is part of profileNavEntry's public signature.
      api(projects.core.framework)
      // ProfileEntry is the public supertype of ProfileEntryImpl.
      api(projects.feature.profileApi)
      implementation(kmp.calf.file.picker)
    }
  }
}

// Robolectric API 37 accesses the JDK file descriptor bridge.
tasks.withType<Test>().configureEach {
  jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}
