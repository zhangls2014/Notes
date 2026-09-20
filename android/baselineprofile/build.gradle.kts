plugins {
  alias(kmp.plugins.android.test)
  alias(kmp.plugins.androidx.baselineprofile)
}

kotlin {
  // 显式声明编译用 JDK：否则回退到运行 Gradle 的 JDK，
  // 而 IDE（Android Studio 自带 JBR）与命令行的默认 JDK 可能不同
  jvmToolchain(kmp.versions.jvm.target.get().toInt())
}

android {
  namespace = "me.zhangls.baselineprofile"
  buildToolsVersion = kmp.versions.android.buildTools.get()

  compileSdk {
    version = release(kmp.versions.android.compileSdk.get().toInt())
  }

  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(kmp.versions.jvm.target.get())
    targetCompatibility = JavaVersion.toVersion(kmp.versions.jvm.target.get())
  }

  defaultConfig {
    minSdk = kmp.versions.android.minSdk.get().toInt()
    targetSdk = kmp.versions.android.targetSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  targetProjectPath = ":androidApp"

  flavorDimensions += listOf("environment")
  productFlavors {
    create("dev") { dimension = "environment" }
  }
}

baselineProfile {
  useConnectedDevices = true
}

dependencies {
  implementation(kmp.androidx.test.ext.junit)
  implementation(kmp.androidx.test.espresso)
  implementation(kmp.androidx.test.uiautomator)
  implementation(kmp.androidx.benchmark.macro.junit4)
}

androidComponents {
  onVariants { v ->
    val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
    v.instrumentationRunnerArguments.put(
      "targetAppId",
      v.testedApks.map { artifactsLoader.load(it)?.applicationId }
    )
  }
}
