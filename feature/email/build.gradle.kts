plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  id("me.zhangls.kmp-koin")
}

kotlin {
  android {
    compilations.withType<com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation>().configureEach {
      isIncludeAndroidResources = true
    }
  }
  sourceSets {
    androidHostTest.dependencies {
      implementation(kmp.junit)
      implementation(kmp.androidx.test.espresso)
      implementation(kmp.androidx.test.ext.junit)
      implementation(kmp.robolectric)
      implementation(kmp.jetbrains.compose.ui.test.junit4)
    }
    commonMain {
      dependencies {
        implementation(projects.core.data)
        implementation(projects.core.theme)
        implementation(projects.core.framework)

        // 依赖 email 契约(api)；实现类 EmailEntryImpl 通过 Koin 绑定到 EmailEntry
        implementation(projects.feature.emailApi)

        // 自适应布局：列表-详情由 Nav3 的场景策略在宿主侧装配，本模块只负责
        // ① 是"列表内容"还是"详情内容"，② 详情页要不要显示返回键（读 LocalListDetailSceneScope）。
        // 因此这里不再需要 adaptive-navigation（旧的 NavigableListDetailPaneScaffold）
        implementation(kmp.jetbrains.compose.material3.window.size)
        implementation(kmp.jetbrains.compose.material3.adaptive)
        implementation(kmp.jetbrains.compose.material3.adaptive.layout)
        implementation(kmp.jetbrains.compose.material3.adaptive.navigation3)

        // Paging3
        implementation(kmp.androidx.paging.compose)

        // Coil
        implementation(kmp.coil.compose)
        implementation(kmp.coil.network.ktor)

        implementation(kmp.calf.file.picker)
      }
    }

    androidMain {
      dependencies {
        implementation(kmp.androidx.activity.compose)
      }
    }
  }
}

// Robolectric API 37 accesses the JDK file descriptor bridge.
tasks.withType<Test>().configureEach {
  jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}
