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
        // 列表-详情场景：mainNavEntries 用 ListDetailSceneStrategy 给 Tab 打窗格标注。
        // Nav3 的"哪个目的地进列表栏、哪个进详情栏"由场景策略决定，宿主不再自建 pane scaffold
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation3)
      }
    }

    commonTest {
      dependencies {
        // 守卫测试只断言纯函数（形态策略），不需要 Compose 运行时或测试宿主
        implementation(kmp.kotlin.test)
      }
    }
  }
}
