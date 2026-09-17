plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  alias(kmp.plugins.android.lint)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // 语言目录需要 AppLanguage 类型（位于 core:model，UI 基础模块不再反向依赖数据层）
        api(projects.core.model)
      }
    }
  }
}
