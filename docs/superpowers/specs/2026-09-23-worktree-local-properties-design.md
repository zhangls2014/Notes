# Worktree 本地构建设计

日期：2026-09-23
状态：已批准，待实施

## 背景

`local.properties` 不受 Git 跟踪，因此新建 Git worktree 时不会出现。当前
`androidApp/build.gradle.kts` 无条件创建读取该文件的 Provider，使原本可选的
release 签名配置间接变成构建前置条件。

## 目标

- 新建 worktree 后，无需复制或链接 `local.properties` 即可执行 Android debug
  构建及不需要签名的其他 Gradle 任务。
- SDK 路径使用机器级 `ANDROID_HOME` 配置，由所有 worktree 共享。
- release 签名信息不进入仓库，并同时支持开发机和 CI。
- 缺少 release 签名信息时不影响 debug 构建；需要签名的 release 构建不得产生
  误导性的已签名产物。

## 方案

删除构建脚本对 `local.properties` 的显式读取。四项签名配置分别从 Gradle 属性
读取，并以同名大写环境变量作为回退：

| Gradle 属性 | 环境变量 |
| --- | --- |
| `notes.signing.path` | `NOTES_SIGNING_PATH` |
| `notes.signing.storePassword` | `NOTES_SIGNING_STORE_PASSWORD` |
| `notes.signing.keyAlias` | `NOTES_SIGNING_KEY_ALIAS` |
| `notes.signing.keyPassword` | `NOTES_SIGNING_KEY_PASSWORD` |

开发机将 Gradle 属性保存在 `~/.gradle/gradle.properties`；CI 使用机密环境变量。
只有四项配置全部存在且非空时才创建并关联 release signing config。SDK 位置不在
项目构建脚本中另行解析，交由 Android Gradle Plugin 从 `ANDROID_HOME` 或 IDE
生成的本地配置发现。

## 文档

README 的本地配置章节改为说明 `ANDROID_HOME` 和用户级 Gradle 属性。保留
`local.properties` 的忽略规则，因为 Android Studio 仍可能自动生成它。

## 验证

1. 在临时隔离环境中隐藏项目根目录的 `local.properties`，确认 Gradle 配置阶段
   不再因文件缺失失败。
2. 设置有效的 `ANDROID_HOME` 后执行 `:androidApp:assembleDebug`。
3. 检查 Git diff，确认未改动或提交任何真实签名凭据。
