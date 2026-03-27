plugins {
  alias(kmp.plugins.jetbrains.kotlin.serialization)
  alias(kmp.plugins.jetbrains.kotlin.multiplatform)
  alias(kmp.plugins.android.kmp.library)
  alias(kmp.plugins.android.lint)
  alias(kmp.plugins.androidx.room)
  alias(kmp.plugins.google.ksp)
  alias(kmp.plugins.koin.compiler)
}

kotlin {
  android {
    namespace = "me.zhangls.data"
    buildToolsVersion = kmp.versions.android.buildTools.get()

    compileSdk = kmp.versions.android.compileSdk.get().toInt()
    minSdk = kmp.versions.android.minSdk.get().toInt()
  }

  listOf(
    iosArm64(),
    iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "dataKit"
      isStatic = true
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        // DataStore
        api(kmp.androidx.datastore.preferences)

        // Json 序列化
        api(kmp.jetbrains.kotlinx.serialization.core)
        api(kmp.jetbrains.kotlinx.serialization.json)

        // database
        implementation(kmp.androidx.room.runtime)
        implementation(kmp.androidx.room.paging)
        implementation(kmp.androidx.sqlite.bundled)

        // Koin
        implementation(project.dependencies.platform(kmp.koin.bom))
        implementation(kmp.koin.core)
        implementation(kmp.koin.annotations)
      }
    }
  }
}

dependencies {
  add("kspAndroid", kmp.androidx.room.compiler)
  add("kspIosArm64", kmp.androidx.room.compiler)
  add("kspIosSimulatorArm64", kmp.androidx.room.compiler)
}

room3 {
  schemaDirectory("$projectDir/schemas")
}