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
        implementation(projects.feature.email)
        // 依赖 main 契约(api)；AppNavHost 引用 MainResult
        implementation(projects.feature.mainApi)
        implementation(projects.feature.main)
        // 依赖 login 契约(api)；AppNavHost 引用 LoginResult
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
