# 应用版本与更新记录

## 版本规则

- Android `versionName` 与 iOS `MARKETING_VERSION` 使用相同的 `MAJOR.MINOR.PATCH`。基线为 `1.0.0`，不补造建立规则之前的历史发布。
- MAJOR 用于重大产品或不兼容变化；MINOR 用于新增用户功能；PATCH 用于修复与小范围优化。高位递增时低位归零。
- 日常需求累计到未发布记录；仅准备发布时确定版本与日期，不因每个提交升级版本。
- Android `versionCode` 与 iOS `CURRENT_PROJECT_VERSION` 分别严格递增，不随版本号重置。重打发布构建也必须使用更高构建号。更新版本时必须同步核对两端配置；版本递增本身不表示已经发布。
- 已发布记录永久保留，仅允许修正事实或文字错误。发布前核对两个平台的版本号与各自构建号，不能把本地编译日期当作发布日期。

## 数据归属与格式

唯一更新内容来源为 `feature/about/src/commonMain/composeResources/files/releases.json`，随安装包分发供离线读取，同时生成官网的已发布历史，不写入数据库。该资源目录仅存应用可消费的静态文件；更新历史集中在 `releases.json`，不按日期复制文件，不清理已发布条目。

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
4. 发布后继续累积下一个未发布版本。历史内容随安装包保留，官网从同一份数据生成公开历史。应用手动检查更新读取各渠道清单，构建可获取后再更新清单，见 [网站部署与应用发布](notes-web-deployment.md#发布新构建)。

## 展示与验收

版本号展开区离线显示与当前安装版本号精确匹配的更新摘要。匹配到草稿时标注「未发布」，无匹配记录时显示「暂无记录」。

「查看更新日志」在应用内加载官网 `/releases/`。页面只展示已发布版本，平台可选 Android 或 iOS，应用内默认匹配当前平台。网页接受 `lang`、`fontScale`、`theme` 和与开源声明一致的配色参数，`embed=1` 隐藏官网导航和网页标题。加载失败可重试。

```bash
./gradlew :feature:about:testAndroidHostTest :feature:about:detekt
./gradlew :composeApp:testAndroidHostTest --tests '*AboutUiTest*' \
  --tests '*OpenSourceNavigationTest*' \
  --tests '*NavBackStackSerializationTest*'
./gradlew :composeApp:compileKotlinIosSimulatorArm64
# 验证 AboutEntry 的实际实现调用
./gradlew :composeApp:iosSimulatorArm64Test
```

未完成的设备与视觉验收统一跟踪于 [待验收清单](testing/pending-acceptance.md#离线更新日志2026-10-30)。

## 更新内容展示顺序

- 当前版本摘要、历史版本卡片和首次启动更新弹窗统一使用 `ReleaseChanges` 渲染。
- 分类固定按「新增（`added`）→ 优化（`improved`）→ 修复（`fixed`）」排列，不依赖 JSON 中各类首次出现的顺序；没有条目的分类不显示。
- 分类标题使用粗体（`FontWeight.Bold`）；正文保持现有字重、字号和间距。
- 同一分类内保留 JSON 中的条目顺序，不按文案或平台重新排序；平台过滤沿用现有逻辑。

2026-10-30 验证：API 37 / Robolectric 的乱序数据回归在修改前显示「修复、新增、优化」并失败，修改后按规定顺序通过；标题布局的 `FontWeight.Bold` 断言通过。更新弹窗、About 页面与导航宿主测试共 32 项、About 模块测试 19 项通过，无失败或跳过；About detekt 和 iOS 模拟器 Kotlin 编译通过。未执行设备视觉验收或更新正式截图基线。


## 新版本首次启动提示

应用根层在登录状态与设置读取完成后，通过 AboutEntry 展示当前版本的更新弹窗；首次安装同样提示，不要求登录。仅匹配当前版本和平台，缺失记录或读取失败时不弹窗；跨版本升级不累计展示历史。内容与更新历史一致：显示发布日期或“未发布”，复用 `ReleaseChanges` 展示分类标题及正文，保持相同分组与间距；沿用应用主题与字号并支持滚动。确认、返回或点击弹窗外关闭后，将当前版本号保存到设置 DataStore；同版本后续启动不再提示，构建号变化不影响提示。清除数据后重新提示，未关闭即退出则下次仍提示。

实施与验证顺序：先验证旧设置兼容和已读持久化，再接入 About 弹窗与根启动协调，最后验证首次展示、关闭、版本切换、无记录，以及 iOS Entry 真实调用。设备与视觉验收单独记录。
