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
        implementation(projects.core.model)
        implementation(projects.core.theme)
        implementation(projects.core.framework)
        // 语言 / 深色模式的设置项元数据（登录页只用这两项，故不依赖 feature:settings）
        implementation(projects.core.preference)

        // 依赖 login 契约(api)；实现类 LoginEntryImpl 通过 Koin 绑定到 LoginEntry
        implementation(projects.feature.loginApi)

        // 登录页的两区排布交给库的窗格脚手架：分区数、栏间留白、**铰链避让**都由
        // PaneScaffoldDirective 决定，本 feature 不再自己算"窗口够不够放两区"。
        // core:theme 已 api 出该库，这里仍显式声明：本模块代码直接引用了它的公开类型
        // （SupportingPaneScaffold / ThreePaneScaffoldValue / AdaptStrategy）。
        implementation(kmp.jetbrains.compose.material3.adaptive.layout)
      }
    }

    commonTest {
      dependencies {
        // 守卫测试只断言"窗格方案"这个纯函数，不需要 Compose 运行时或测试宿主
        implementation(kmp.kotlin.test)
      }
    }
  }
}
