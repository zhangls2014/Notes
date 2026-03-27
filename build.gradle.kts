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
}