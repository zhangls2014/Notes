plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.android.fusedlibrary) apply false
  alias(libs.plugins.android.test) apply false
  alias(libs.plugins.androidx.baselineprofile) apply false
  alias(libs.plugins.androidx.room) apply false
  alias(libs.plugins.jetbrains.serialization) apply false
  alias(libs.plugins.jetbrains.compose.compiler) apply false
  alias(libs.plugins.google.ksp) apply false
  alias(libs.plugins.google.hilt) apply false
}