plugins {
  alias(kmp.plugins.android.application) apply false
  alias(kmp.plugins.android.library) apply false
  alias(kmp.plugins.android.fusedlibrary) apply false
  alias(kmp.plugins.android.test) apply false
  alias(kmp.plugins.androidx.baselineprofile) apply false

  alias(kmp.plugins.android.kmp.library) apply false
  alias(kmp.plugins.android.lint) apply false
  alias(kmp.plugins.androidx.room) apply false
  alias(kmp.plugins.jetbrains.compose) apply false
  alias(kmp.plugins.jetbrains.kotlin.multiplatform) apply false
  alias(kmp.plugins.jetbrains.kotlin.serialization) apply false
  alias(kmp.plugins.jetbrains.kotlin.compose.compiler) apply false
  alias(kmp.plugins.google.ksp) apply false
  alias(kmp.plugins.koin.compiler) apply false

  alias(kmp.plugins.buildKonfig) apply false

  // 只在此锁定版本，供 build-logic 的 me.zhangls.detekt 约定插件在各模块上应用。
  // 先前此处是 apply（不带 apply false），导致 detekt 只作用于根项目；根项目无源集，
  // 任务恒为 NO-SOURCE，config/detekt/detekt.yml 从未对任何模块代码生效过。
  alias(kmp.plugins.detekt) apply false
}
