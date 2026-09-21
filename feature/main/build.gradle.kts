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

        // 自适应布局：导航套件（Rail / 底部导航栏）与窗口尺寸类。
        // adaptive-layout 与 adaptive-navigation 由本模块的代码直接用不到，故不显式声明
        implementation(kmp.jetbrains.compose.material3.window.size)
        implementation(kmp.jetbrains.compose.material3.adaptive)
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation.suite)
      }
    }
  }
}
