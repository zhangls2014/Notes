# 应用版本与更新记录

## 版本规则

- Android `versionName` 与 iOS `MARKETING_VERSION` 使用相同的 `MAJOR.MINOR.PATCH`。基线为 `1.0.0`，不补造建立规则之前的历史发布。
- MAJOR 用于重大产品或不兼容变化；MINOR 用于新增用户功能；PATCH 用于修复与小范围优化。高位递增时低位归零。
- 日常需求累计到未发布记录；仅准备发布时确定版本与日期，不因每个提交升级版本。
- Android `versionCode` 与 iOS `CURRENT_PROJECT_VERSION` 分别严格递增，不随版本号重置。重打发布构建也必须使用更高构建号。更新版本时必须同步核对两端配置；版本递增本身不表示已经发布。
- 已发布记录永久保留，仅允许修正事实或文字错误。发布前核对两个平台的版本号与各自构建号，不能把本地编译日期当作发布日期。

## 数据归属与格式

唯一更新内容来源为 `feature/about/src/commonMain/composeResources/files/releases.json`，随安装包分发，离线读取，不写入数据库。该资源目录仅存应用可消费的静态文件；更新历史集中在 `releases.json`，不按日期复制文件，不清理已发布条目。

顶层为版本数组，每项包含：

| 字段 | 约束 |
|---|---|
| `version` | 唯一的三段非负整数版本号；不带前导零 |
| `date` | 正式发布日期 `YYYY-MM-DD`；`null` 表示未发布 |
| `changes` | 非空条目数组 |
| `type` | `added`（新增）、`improved`（优化）、`fixed`（修复） |
| `platforms` | 非空、无重复的 `android` / `ios` 列表 |
| `text` | 非空的 `en`、`zh` 文案；描述用户可感知的变化，不记录内部提交流水 |

最多保留一个未发布版本，其版本必须高于所有已发布版本。App 按数字版本倒序排列，按当前平台过滤；没有适用条目的版本不显示。中文环境使用中文，其余语言回退英文。未发布条目明确标记“未发布”，不混充已发布历史。

## 每次变更与发布流程

1. 每次用户可感知的功能、优化或修复，必须在同一需求中更新未发布记录；纯内部整理无须凑条目。没有未发布记录时新建一项，按改动选择下一个版本。
2. 发布前将未发布条目的 `date` 改为实际发布日期，核对两个平台版本与构建号；不得由 AI 自动发布。
3. 执行 About 模块测试、Android 宿主页面行为测试及 iOS 编译；检查本地 JSON 与版本配置的一致性。页面设备验收和编译分别报告。
4. 发布后继续累积下一个未发布版本。历史内容随安装包保留；此机制不承担联网检查更新、下载或安装。

## 展示与验收

关于应用保留品牌卡与检查更新入口。点击版本号展开当前安装版本的更新内容和“查看历史更新”入口；构建号只用于平台发布管理，不在关于应用展示。页面不另设顶层更新日志栏目。摘要只匹配当前安装版本，没有对应记录时显示暂无记录，不借用最新版本内容。历史更新使用独立导航页面，以可滚动的版本网格展示：复用窗口分级，小于 Expanded 时单列、Expanded/Large 双列、ExtraLarge 三列；按从左到右、再从上到下的顺序展示版本。每列内容宽度基于 `ContentWidth.Form`，卡片内外边距与列间距均保持 16dp，支持返回、主题及字体缩放。读取失败显示错误提示，不伪造空历史；再次进入可重新读取。

实施顺序与验证：先定义规则与索引，再实现 JSON 解析和数据校验（版本唯一、排序、平台与语言），随后接入列表页面并测试进入/返回，最后执行 `:feature:about:testAndroidHostTest`、相关 About 宿主测试和 iOS 编译。正式截图基线不在本次更新范围内。

```bash
./gradlew :feature:about:testAndroidHostTest :feature:about:detekt
./gradlew :composeApp:testAndroidHostTest --tests '*AboutUiTest*' \
  --tests '*AboutNoticesNavigationTest*' --tests '*OpenSourceNavigationTest*' \
  --tests '*NavBackStackSerializationTest*' --tests '*ReleaseHistoryAdaptiveTest*'
./gradlew :composeApp:compileKotlinIosSimulatorArm64
# 修改 AboutEntry 时必须执行真实入口调用回归
./gradlew :composeApp:iosSimulatorArm64Test
```

未完成的设备与视觉验收统一跟踪于 [待验收清单](testing/pending-acceptance.md#离线更新日志2026-10-30)。
