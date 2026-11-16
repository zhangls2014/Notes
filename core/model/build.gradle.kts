@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
  id("me.zhangls.kmp-library")
}

kotlin {
  wasmJs { browser() }

  sourceSets {
    commonMain {
      dependencies {
        api(kmp.jetbrains.kotlinx.serialization.core)
      }
    }
  }
}
