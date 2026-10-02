plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-koin")
  alias(kmp.plugins.android.lint)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // 公共领域模型（AppLanguage / DarkThemeConfig / MailboxType 等）
        api(projects.core.model)

        // 存储实现：Room 整体下沉到 core:database。
        // 用 implementation —— 不向下游传递，feature 层因此看不到任何 Room 类型
        implementation(projects.core.database)

        // 分页（仓库的分页查询与 PagingData 映射）
        implementation(kmp.androidx.paging.common)

        // DataStore（仅供本模块内部使用，不向下游暴露）
        implementation(kmp.androidx.datastore.preferences)

        // Json 序列化
        api(kmp.jetbrains.kotlinx.serialization.core)
        api(kmp.jetbrains.kotlinx.serialization.json)
      }
    }

    commonTest {
      dependencies {
        implementation(kmp.kotlin.test)
        implementation(kmp.jetbrains.kotlinx.coroutines.test)
      }
    }
  }
}
