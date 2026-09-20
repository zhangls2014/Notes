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
        // 设置项的展示元数据与配套控件（PreferenceRow / ProvidePreferenceLocals）
        implementation(projects.core.preference)

        // 依赖 settings 契约(api)；实现类 SettingsEntryImpl 通过 Koin 绑定到 SettingsEntry
        implementation(projects.feature.settingsApi)
      }
    }
  }
}
