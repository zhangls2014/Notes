# Android Fused Library

该模块为 Android 端 Fused Library 实验性打包，能力仍不稳定，仅供测试。具体功能请查看[官方文档](https://developer.android.com/build/publish-library/fused-library?hl=zh-cn#kts)。

目录约定：`login/` 保存登录融合模块配置；生成的 AAR、依赖报告及本地 Maven 仓库均位于该模块的 `build/`，不提交生成产物。内部模块显式融合，第三方依赖由发布元数据传递，不把 AAR 当成包含全部依赖的独立文件。

登录融合包显式包含 model、database、data、theme、preference、framework、login-api 和 login 八个内部模块。未融合的第三方依赖通过 `fusedLibraryComponent` 生成的 POM 和 Gradle Module Metadata 传递；新增内部依赖时须同步核对融合报告，不能残留未发布的项目坐标。

AGP 9.3.1 会按 GAV 重建发布依赖，丢失平台依赖属性，导致 Koin BOM 在独立消费时被当成普通库并解析失败。当前配置从实际解析结果识别平台组件，在 `runtimePublication` 恢复其 `platform` 属性，使 Module Metadata 保留类型、POM 使用 BOM import。此处依赖 AGP 的内部配置名称，升级插件时必须重跑独立消费验证。

打包方法：执行 assemble 命令，例如：

```bash
./gradlew :android:output:login:assemble
```

## 本地 Maven 消费验证

```bash
./gradlew :android:output:login:publishReleasePublicationToLocalBuildRepository \
  :android:output:login:report
```

本地测试坐标为 `me.zhangls:notes-login:0.0.0-local`，与应用版本独立；这不是正式发布版本。仓库位于 `login/build/repo/`，不写入 `~/.m2`。发布任务仅写入该构建目录，不配置远程发布。

在独立 Android 消费工程的 `settings.gradle.kts` 中，将该目录作为 Maven 仓库，并保留 `google()` 与 `mavenCentral()`；模块中仅添加 `implementation("me.zhangls:notes-login:0.0.0-local")`，不要添加源码项目依赖或直接复制 AAR。消费工程应使用与本项目兼容的 Kotlin、Compose 编译器、JDK 和 Android SDK 配置；当前版本由根目录 `gradle/kmp.versions.toml` 维护。

验证须检查：

- `login/build/reports/fused_library_report/single/report.json` 包含八个内部模块，外部依赖中没有 `Notes.*:*:unspecified`。
- 发布的 POM / Module Metadata 不残留未发布的内部模块坐标；AAR 包含 model、数据库及 Room 生成实现、登录入口和 Compose 资源。
- 独立工程只使用 Maven 坐标，编译调用 `LoginEntry.Screen()` 并引用 `AppLanguage`、`AppDatabase`，执行 `assembleDebug`。

构建消费验证不代表登录运行、Koin 装配、数据库初始化或设备 UI 已验收。宿主仍须提供登录入口所需的依赖注入和窗口环境；第三方库不全部融合进 AAR。

2026-10-30 验证：本地 Maven 发布、融合报告检查、AAR 类和资源检查、BOM 元数据检查均通过。仓库外 Android 应用仅声明上述坐标，调用登录 Compose 入口并引用 model / database 类型，`assembleDebug` 通过。消费工程复用当前版本目录与 Build Tools 37；首次完整打包在默认 512 MiB Gradle 堆下出现原生文件监听异常、生成堆转储并中断，最终使用 `--no-watch-fs --no-daemon -Dorg.gradle.jvmargs="-Xmx4g -Dfile.encoding=UTF-8 -XX:+UseParallelGC"` 完成验证。未运行设备或验证登录、数据库和依赖注入初始化行为。
