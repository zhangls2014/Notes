plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  alias(kmp.plugins.android.lint)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // 图标（Icons.Rounded.*）与 ThemeColor 属于 UI 基础能力，仍留在 core:theme，
        // 本模块只依赖（不搬迁图标，避免 core:theme 图标集出现缺口）
        api(projects.core.theme)

        // 设置取值枚举（AppLanguage / DarkThemeConfig / FontSizeConfig）作为元数据的类型参数
        // 出现在本模块的公开 API（PreferenceOption<T> / PreferenceSpec.Select<T>），须 api 暴露。
        // 它们只能放 core:model —— core:data 的契约需要它们，而数据层不能依赖含 Compose 的本模块
        api(projects.core.model)

        // 列表行控件的实现库（SwitchPreference / ListPreference / Preference）。
        // 刻意用 implementation：消费方只用本模块的 PreferenceUiModel / PreferenceRow /
        // ProvidePreferenceLocals，底层类型不进它们的编译期类路径 —— 与 core:data 对
        // core:database 的手法一致，靠 Gradle 的可见性边界而不是约定来保证解耦
        implementation(kmp.compose.preference)
      }
    }
  }
}
