import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  id("me.zhangls.kmp-koin")
  alias(kmp.plugins.buildKonfig)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // Module
        implementation(projects.core.data)
        implementation(projects.core.model)
        implementation(projects.core.theme)
        implementation(projects.core.preference)
        implementation(projects.core.network)
        implementation(projects.core.framework)
        // 依赖 email 契约(api)；AppNavHost 引用 EmailDetailDestination
        implementation(projects.feature.emailApi)
        implementation(projects.feature.email)
        // 依赖 main 契约(api)；AppNavHost 引用 TabDestination
        implementation(projects.feature.mainApi)
        // 外壳 AppShell 在实现模块，由组合根装配
        implementation(projects.feature.main)
        // 依赖 login 契约(api)；AppNavHost 引用 LoginResult 与 LoginDestination
        implementation(projects.feature.loginApi)
        implementation(projects.feature.login)
        implementation(projects.feature.settings)

        implementation(kmp.androidx.lifecycle.viewmodel.navigation3)
        // 列表-详情场景策略：NavDisplay 的 sceneStrategies 用它装配"列表栏 + 详情栏"，
        // 于是"首页分栏 / 收藏页整页替换"这两种表现合一，且两端 directive 口径一致
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation3)
        // 场景策略的 directive 由组合根用 LocalWindowAdaptiveInfo 现算（而不是吃库的默认参数），
        // 于是同一帧里只有一个窗口读取点
        implementation(kmp.jetbrains.compose.material3.adaptive.layout)
      }
    }

    commonTest.dependencies {
      implementation(kmp.kotlin.test)
    }

    androidMain.dependencies {
      // Core
      implementation(kmp.androidx.activity.compose)
      implementation(kmp.androidx.lifecycle.compose)
      implementation(kmp.androidx.viewmodel.compose)
    }
  }
}

koinCompiler {
  userLogs = true
  compileSafety = false
  unsafeDslChecks = false
}

/**
 * 编译期常量配置
 */
buildkonfig {
  packageName = "me.zhangls.entry"
  defaultConfigs {
    buildConfigField(STRING, "BASE_URL", "https://www.mxnzp.com/")
  }
}
