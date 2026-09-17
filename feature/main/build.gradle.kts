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
        implementation(projects.core.network)
        implementation(projects.core.framework)

        // 依赖 email 契约(api)；HomeScreen / FavoritesScreen 由 Koin 提供 EmailEntry 实现
        implementation(projects.feature.emailApi)

        // 依赖 main 契约(api)；实现类 MainEntryImpl 通过 Koin 绑定到 MainEntry
        implementation(projects.feature.mainApi)

        implementation(projects.feature.settingsApi)

        // 自适应布局
        implementation(kmp.jetbrains.compose.material3.window.size)
        implementation(kmp.jetbrains.compose.material3.adaptive)
        implementation(kmp.jetbrains.compose.material3.adaptive.layout)
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation)
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation.suite)
      }
    }
  }
}
