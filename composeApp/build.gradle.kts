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
