import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
  id("com.google.devtools.ksp")
  id("io.insert-koin.compiler.plugin")
}

// 本插件由模块与 me.zhangls.kmp-library 组合应用（library 先应用，Kotlin 扩展已存在）
val kmp = extensions.getByType<VersionCatalogsExtension>().named("kmp")

// 源集依赖 lambda 内无 platform() 扩展，先在项目级 DependencyHandler 上构造 BOM provider
val koinBom = dependencies.platform(kmp.findLibrary("koin-bom").get())

configure<KotlinMultiplatformExtension> {
  sourceSets {
    commonMain {
      dependencies {
        // api：BOM 版本约束需向下游模块传播（koin-* 别名本身无版本号）
        api(koinBom)
        implementation(kmp.findLibrary("koin-core").get())
        implementation(kmp.findLibrary("koin-annotations").get())
      }
    }
  }
}
