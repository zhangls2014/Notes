plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  alias(kmp.plugins.android.lint)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // 窗口形态：WindowAdaptiveInfo / Posture 出现在本模块的公开 API 里
        // （LocalWindowAdaptiveInfo、ProvideWindowAdaptiveInfo），故用 api 传播给消费方。
        // 消费方因此不必各自声明 adaptive，也就不再可能各自调 currentWindowAdaptiveInfo*。
        api(kmp.jetbrains.compose.material3.adaptive)
        // WindowSizeClass：现于 ProvideWindowAdaptiveInfo 的参数类型与本模块的语义化读法里，
        // 同样是公开 API 的一部分。显式声明而不是吃 adaptive 的传递依赖 ——
        // 上游收窄依赖时，未声明的使用会以"未解析的引用"突然失败。
        api(kmp.androidx.window.core)
      }
    }
  }
}
