plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-koin")
  alias(kmp.plugins.androidx.room)
  alias(kmp.plugins.android.lint)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // 公共领域模型（AppLanguage 等）
        api(projects.core.model)

        // DataStore
        api(kmp.androidx.datastore.preferences)

        // Json 序列化
        api(kmp.jetbrains.kotlinx.serialization.core)
        api(kmp.jetbrains.kotlinx.serialization.json)

        // database
        implementation(kmp.androidx.room.runtime)
        implementation(kmp.androidx.room.paging)
        implementation(kmp.androidx.sqlite.bundled)
      }
    }

    commonTest {
      dependencies {
        implementation(kmp.kotlin.test)
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
