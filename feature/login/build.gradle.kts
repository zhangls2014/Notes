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
      }
    }

    commonTest {
      dependencies {
        // 守卫测试只断言排布决策这个纯函数，不需要 Compose 运行时或测试宿主
        implementation(kmp.kotlin.test)
      }
    }
  }
}
