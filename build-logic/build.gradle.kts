plugins {
  `kotlin-dsl`
}

dependencies {
  // 预编译脚本插件 apply 的插件需要以 marker 工件形式进入本构建的 plugin classpath
  compileOnly(kmp.kotlin.gradle.plugin)
  compileOnly(kmp.kotlin.serialization.plugin)
  compileOnly(kmp.kotlin.compose.plugin)
  compileOnly(kmp.android.gradle.plugin)
  compileOnly(kmp.compose.gradle.plugin)
  compileOnly(kmp.ksp.gradle.plugin)
  compileOnly(kmp.koin.compiler.gradle.plugin)
}
