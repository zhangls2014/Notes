# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个 Kotlin Multiplatform (KMP) 应用，使用 Kotlin、Compose Multiplatform、Koin 和模块化架构。应用主要功能是邮件管理，包含首页、收藏页和设置页。共享逻辑与 UI 在 `commonMain` 中实现，并在 Android 与 iOS 端复用。

## 构建和运行

### 构建项目

```bash
./gradlew build
```

### 运行 Android

```bash
./gradlew :androidApp:installDebug
```

### 运行 iOS

使用 Xcode 打开 `iosApp/iosApp.xcodeproj` 运行。

### 运行测试

```bash
./gradlew test
./gradlew :androidApp:connectedAndroidTest
```

## 项目架构

### 模块结构

- `androidApp`: Android 入口应用
- `iosApp`: iOS 入口应用
- `composeApp`: 共享应用入口与跨平台 UI/导航
- `core:data`: 数据层，包含数据源和仓库
- `core:theme`: 主题和样式
- `core:network`: 网络层
- `core:framework`: 框架层，包含基础组件和工具类
- `feature:main`: 主功能模块，包含主屏幕和邮件列表
- `feature:login`: 登录功能
- `feature:settings`: 设置功能
- `android:baselineprofile`: Android 基线配置文件

### 主要组件

#### 主屏幕 (MainScreen)

位于 `feature/main/src/commonMain/kotlin/me/zhangls/main/MainScreen.kt`，包含三个标签页：

- HOME: 首页，显示邮件列表
- FAVORITES: 收藏页
- SETTINGS: 设置页

#### 首页 (HomeScreen)

位于 `feature/main/src/commonMain/kotlin/me/zhangls/main/home/HomeScreen.kt`，使用 `EmailList` 组件显示邮件列表。

#### 邮件列表 (EmailList)

位于 `feature/main/src/commonMain/kotlin/me/zhangls/main/compose/EmailList.kt`，使用分页数据加载：

- 初始加载时显示文本 "Waiting for items to load from the backend"
- 加载更多时显示 `CircularProgressIndicator`

### 数据加载

应用使用分页数据加载（Paging），通过 `EmailViewModel` 管理：

- `emailPaging` 流提供分页数据
- 使用 `collectAsLazyPagingItems()` 在 Compose 中收集数据
- 加载状态通过 `LoadState` 监控

## 注意事项

- 项目使用 Koin 进行依赖注入
- 使用 Compose Multiplatform 进行 UI 构建
- 采用 MVI (Model-View-Intent) 架构模式
- 数据通过分页加载，支持无限滚动
- 支持多种设备尺寸和方向适配
