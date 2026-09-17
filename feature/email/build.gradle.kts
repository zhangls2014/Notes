plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  id("me.zhangls.kmp-koin")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core.data)
        implementation(projects.core.theme)
        implementation(projects.core.framework)

        // 依赖 email 契约(api)；实现类 EmailEntryImpl 通过 Koin 绑定到 EmailEntry
        implementation(projects.feature.emailApi)

        // 自适应布局
        implementation(kmp.jetbrains.compose.material3.window.size)
        implementation(kmp.jetbrains.compose.material3.adaptive)
        implementation(kmp.jetbrains.compose.material3.adaptive.layout)
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation)
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation.suite)

        // Paging3
        implementation(kmp.androidx.paging.compose)

        // Coil
        implementation(kmp.coil.compose)
        implementation(kmp.coil.network.ktor)

        implementation(kmp.calf.file.picker)
      }
    }

    androidMain {
      dependencies {
        implementation(kmp.androidx.activity.compose)
      }
    }
  }
}
