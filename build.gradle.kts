plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.android.fusedlibrary) apply false
  alias(libs.plugins.android.test) apply false

  alias(libs.plugins.androidx.baselineprofile) apply false
  alias(libs.plugins.androidx.room) apply false

  alias(libs.plugins.jetbrains.kotlin.serialization) apply false
  alias(libs.plugins.jetbrains.kotlin.compose.compiler) apply false

  alias(libs.plugins.google.ksp) apply false

  alias(libs.plugins.koin.compiler) apply false
}