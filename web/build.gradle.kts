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
  val distribution = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
  val skiko = layout.buildDirectory.file("compose/skiko-wasmJs-runtime/skiko.wasm")
  val environmentNames = listOf(
    "NOTES_SITE_BASE_URL", "NOTES_APK_BASE_URL", "NOTES_ANDROID_SIGNING_SHA256",
    "NOTES_IOS_APP_ID", "CF_PAGES_BRANCH", "NOTES_PUBLISHED_BUILDS_FILE", "NOTES_WEB_OUTPUT_DIR",
  )
  environmentNames.forEach { name ->
    val value = providers.environmentVariable(name)
    inputs.property(name, value.orElse(""))
    // Exec inherits its environment from the daemon; supply the tracked value explicitly.
    value.orNull?.let { environment(name, it) }
  }
  val repositoryDirectory = rootProject.layout.projectDirectory
  val publishedBuilds = providers.environmentVariable("NOTES_PUBLISHED_BUILDS_FILE")
    .map { repositoryDirectory.file(it) }.orElse(layout.projectDirectory.file("published-builds.json"))
  val outputDirectory = providers.environmentVariable("NOTES_WEB_OUTPUT_DIR")
    .map { repositoryDirectory.dir(it) }.orElse(layout.projectDirectory.dir("dist"))
  inputs.files("build.mjs", "notice-data.mjs", "device-viewport.js", "open-source/dependencies.json")
  inputs.dir("open-source/licenses")
  inputs.file(repositoryDirectory.file("feature/about/src/commonMain/composeResources/files/releases.json"))
  inputs.file(repositoryDirectory.file("core/theme/src/commonMain/kotlin/me/zhangls/theme/Color.kt"))
  inputs.file(publishedBuilds)
  inputs.dir(distribution)
  inputs.file(skiko)
  outputs.dir(outputDirectory)
  val node = rootProject.extensions.getByType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>()
  commandLine(
    node.executable.get(), "web/build.mjs", "--wasm",
    distribution.get().asFile,
    skiko.get().asFile,
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
