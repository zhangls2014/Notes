import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  id("me.zhangls.kmp-koin")
  alias(kmp.plugins.buildKonfig)
  alias(kmp.plugins.roborazzi)
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
      implementation(kmp.androidx.paging.common)
      implementation(kmp.robolectric)
      implementation(kmp.roborazzi)
      implementation(kmp.jetbrains.compose.ui.test.junit4)
    }
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
        implementation(projects.feature.profileApi)
        implementation(projects.feature.profile)
        implementation(projects.feature.aboutApi)
        implementation(projects.feature.about)

        implementation(kmp.androidx.lifecycle.viewmodel.navigation3)
        implementation(kmp.jetbrains.navigation3.ui)
        implementation(kmp.androidx.navigationevent.compose)
        // 列表-详情场景策略：NavDisplay 的 sceneStrategies 用它装配"列表栏 + 详情栏"，
        // 于是"首页分栏 / 收藏页整页替换"这两种表现合一，且两端 directive 口径一致
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation3)
        // 注意这里不再声明 adaptive-layout：directive 的**计算**已随 LocalPaneScaffoldDirective
        // 收进 core:theme，本模块只传递它的实例，代码里不再出现 adaptive.layout 的类型。
      }
    }

    iosTest.dependencies {
      implementation(projects.feature.settingsApi)
    }

    commonTest.dependencies {
      implementation(kmp.kotlin.test)
      implementation(kmp.jetbrains.kotlinx.coroutines.test)
    }

    androidMain.dependencies {
      // Core
      implementation(kmp.androidx.core)
      implementation(kmp.androidx.activity.compose)
      implementation(kmp.androidx.lifecycle.compose)
      implementation(kmp.androidx.viewmodel.compose)
    }
  }
}

koinCompiler {
  userLogs = true
  compileSafety = true
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

// UI rendering is memory intensive; keep tests sequential and separate from device services.
tasks.withType<Test>().configureEach {
  maxHeapSize = "4g"
  // Robolectric API 37 shared-memory support accesses the JDK file descriptor bridge.
  jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
  maxParallelForks = 1
  systemProperty("notes.screenshots.candidates", providers.gradleProperty("notes.screenshots.candidates").orElse("false").get())
}

roborazzi {
  outputDir.set(layout.projectDirectory.dir("src/androidHostTest/screenshots"))
}
