# 个人信息模块拆分实施计划

日期：2026-10-03。状态：已实施并验证，用户已授权提交并合入本地 master。用户已确认独立 profile-api / profile 模块方案。

目标：将个人信息从 settings 拆出，保持页面、两个入口、头像修改、隐藏导航和返回恢复行为。

## 模块与文件归属

- `feature:profile-api`：ProfileEntry、ProfileDestination/ProfileOrigin、ProfileNavigation；使用 kmp-compose-api 约定插件。保留旧目的地的序列化名称以兼容已有返回栈。
- `feature:profile`：ProfileScreen、mvi、AvatarStorage/AvatarSaver/AvatarUpdater、平台实现、页面资源与 14 项宿主测试；实现依赖自身 API。
- `feature:settings`：设置页面和个人信息点击事件，移除页面、文件选择依赖和导航贡献。
- `feature:main`：依赖 profile-api 传递 ProfileOrigin；不依赖 profile 实现。
- `composeApp`：依赖并装配 profile API/实现、Koin 与导航；更新 Native Entry ABI 和目的地注册测试。
- Core 的用户数据、原子头像更新、共享头像 UI、设置项静态元数据保持原归属。

## 实施与验证

- [x] 添加并验证旧序列化名称的兼容回归，保留已有行为测试作为拆分基线。
- [x] 登记两个新模块，迁移文件、包名、资源、测试和依赖。
- [x] 添加 ProfileEntry 实现与独立 Koin 导航贡献，更新组合根和调用方。
- [x] 检查旧 settings 个人信息引用清零和 Feature 实现依赖边界。
- [x] 运行 profile 宿主测试、组合根导航回归、iOS 编译/模拟器测试、Android 构建和 lint；安装到模拟器。
- [x] 更新架构文档、审计状态及设计记录，独立只读审查。

## 验证结果

profile 宿主测试 14 项、组合根导航与序列化宿主回归 18 项、iOS 模拟器测试 25 项均通过，无失败、错误或跳过。iOS 编译通过；Android 全部 Debug 变体构建通过，devFullDebug lint 为 0 Error / 12 Warning。首次整包构建在 dex 合并时内存不足，按仓库规定使用 8GB 堆配置重试成功。devFullDebug 已安装到 emulator-5554，启动命令返回 Status: ok。

独立只读审查未发现具体缺陷。旧 settings 个人信息类型引用仅保留在兼容序列化名称及其回归用例中。页面与头像功能的迁移保持原行为；本次未重新执行系统图片选择器与真实读屏的人工验收。

按用户后续指示将本次拆分保存为一个新提交，并 fast-forward 合入本地 master；保留已有提交历史，不推送远端。
