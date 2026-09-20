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
        // MailboxType 出现在 EmailEntity / Converters 的签名中
        api(projects.core.model)

        // api：`RoomDatabase` 是 `AppDatabase` 的父类型，Kotlin 要求父类型对使用方可见，
        // 故 core:data 必须能在 classpath 上看到 room3-runtime。
        // 这不影响隔离——真正挡住 feature 的是 core:data → 本模块的 implementation 依赖
        api(kmp.androidx.room.runtime)
        implementation(kmp.androidx.room.paging)
        implementation(kmp.androidx.sqlite.bundled)

        // RecipientIdsCodec 的 JSON 编解码
        implementation(kmp.jetbrains.kotlinx.serialization.json)
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
