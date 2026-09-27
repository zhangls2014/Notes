# Tab 导航试用版设计与实施计划

目标：保留独立 Tab Entry，取消 Tab 返回历史，隔离 Tab 切换和详情转场。

已确认方向：用户在方案讨论后要求完成一版体验。第一版只持有活动 Tab 的栈，切回其他 Tab 到根页面，不引入多返回栈；重复点击当前 Tab 不改变详情。根页面返回交还平台。共享 AppShell 和根部列表—详情 SceneStrategy 保留。

实现：抽取内部 NavHandler 到 composeApp，根按 Tab 或详情 scene 决定，登录守卫先执行。selectTab 专门处理顶层切换。NavDisplay 用栈根作为组合 key，根改变直接替换内容并释放旧动画和 Entry；同根 push/pop 保留原有动画。深链与登录恢复建立正确的所属 Tab 栈。

- [x] 添加返回、Tab 根、详情归属、登录恢复的回归测试，先验证旧行为失败。
- [x] 实现 NavHandler 与导航显示隔离，移除 Tab 场景转场推断。
- [x] 增加重复点击、跨 Tab 详情、退出及切换中的 UI 回归检查。
- [x] 同步架构与路由注释，保留用户原有文档改动。
- [x] 串行执行导航测试、相关 UI 测试、Android 构建与 iOS 编译；有设备时安装体验。

验收：收藏/设置根栈没有首页；详情返回原列表；切 Tab 不呈现旧页面转场；当前 Tab 重选无副作用；受保护深链登录后回到正确列表与详情。

验证结果：旧实现的 4 个导航测试中 3 个按预期失败；修复后 8 个导航测试、3 个序列化测试、5 个 UI 场景通过（参数化 UI 中另外 46 项因尺寸筛选跳过）。UI 覆盖 400dp / 900dp 的根返回接管、收窄后详情返回、开启动画时切 Tab；已有预测返回后搜索状态回归也通过。Android assembleDebug 与 iOS Simulator Arm64 编译通过。未执行全量截图矩阵。

## 第二版：保留 Tab 状态

用户要求切回来恢复滚动位置和选中状态。改为每个 Tab 独立保存返回栈，Entry 装饰器在 Tab 内容显示区外持续存在；仅当前 Tab 的 entries 交给 NavDisplay，继续用当前根隔离跨 Tab 转场。rememberNavBackStack 保存栈与当前根，SaveableStateHolder 保存列表位置，Entry ViewModelStore 保留多选等内存状态。退出登录清空所有栈，登录 Entry 在成功后释放。不启用 EmailViewModel 的跨进程状态保存。

- [x] 更新导航测试验证切回来恢复详情、各 Tab 独立、退出登录清理。
- [x] 实现多栈状态与持久装饰器，保持活动 NavDisplay 的返回行为。
- [x] UI 验证滚动、多选、详情恢复和快速切换；同步现有测试的新行为。
- [x] 更新架构文档，构建 Android/iOS 并安装模拟器。

第二版重建测试发现并修复：首次加载指示作为唯一 LazyColumn 条目时会把已恢复的索引从 5 压回 0；将刷新空态指示移到列表外后，状态重建测试通过。普通切换的滚动、多选和详情恢复分别在 400dp / 900dp 验证。未启用 EmailViewModel 的跨进程状态持久化。

第二版最终验证：导航、序列化、Tab 状态及重建恢复、手机/宽屏页面流程测试通过；Android assembleDebug 与 iOS Simulator Arm64 编译通过。已覆盖安装到 emulator-5554，保留应用数据。
