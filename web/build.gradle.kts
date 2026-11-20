@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
  alias(kmp.plugins.jetbrains.kotlin.multiplatform)
  alias(kmp.plugins.jetbrains.kotlin.serialization)
  id("me.zhangls.kmp-compose")
}

kotlin {
  wasmJs {
    browser {
      testTask { useKarma { useChromeHeadless() } }
      commonWebpackConfig {
        outputFileName = "notes.js"
      }
    }
    binaries.executable()
  }
  sourceSets {
    wasmJsTest.dependencies { implementation(kmp.kotlin.test) }
    wasmJsMain.dependencies {
      implementation(projects.core.model)
      implementation(projects.core.theme)
      implementation(kmp.jetbrains.navigation3.ui)
      implementation(kmp.jetbrains.compose.material3.adaptive.navigation3)
      implementation(kmp.jetbrains.kotlinx.serialization.json)
    }
  }
}

tasks.register<Exec>("buildSite") {
  group = "build"
  description = "Builds the public website and its distribution files."
  dependsOn("wasmJsBrowserDistribution")
  workingDir = rootProject.projectDir
  val node = rootProject.extensions.getByType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>()
  commandLine(
    node.executable.get(), "web/build.mjs", "--wasm",
    layout.buildDirectory.dir("dist/wasmJs/productionExecutable").get().asFile,
    layout.buildDirectory.file("compose/skiko-runtime-processed-wasmjs/skiko.wasm").get().asFile,
  )
}

compose.resources {
  packageOfResClass = "me.zhangls.web.resources"
}

// Tool distributions are resolved by the settings repositories.
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
  extensions.configure<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec> {
    downloadBaseUrl.set(null as String?)
  }
}
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
  rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec> {
    downloadBaseUrl.set(null as String?)
  }
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
  extensions.configure<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec> {
    downloadBaseUrl.set(null as String?)
  }
}
