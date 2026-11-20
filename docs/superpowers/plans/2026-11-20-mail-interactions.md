# 邮件刷新与详情布局交付记录

状态：实现与自动回归完成；设备视觉验收待完成。方案由维护者确认，本文保留最终行为与有效验证证据，过程文件未删除。

## 最终实现

- 首页和收藏通过现有 Paging refresh() 支持下拉刷新，包括空列表。保留刷新失败与追加失败时的既有内容和重试入口。
- 继续使用现有每页 20 封的分页实现，不改服务端与数据库。回归使用 45 封远端假数据验证追加、失败重试、末页停止及从第一页刷新。
- 保留回复、回复全部与转发，统一使用 FlowRow 排列，空间不足时换行。
- 详情顶部仅显示当前邮件主题，加载与错误期间留空，长标题单行省略。
- 双栏未选择邮件的提示在真实标题栏后的正文区域居中。列表与详情空态统一使用 bodyLarge/outline。
- 非空列表且刷新成功、追加结束时显示“已显示全部邮件”（All emails shown），使用 bodySmall/onSurfaceVariant，两侧为 3dp 装饰圆点，与文字间隔 8dp。空列表、刷新中或失败、追加中或失败不显示完成提示。
- 按维护者最终要求，完成提示上下均为 8dp，不预留 FAB 高度或间距；保留 Scaffold 原有安全区。加载与失败重试页脚按实际 FAB 高度避让，收藏不保留按钮区域。

归属：列表、详情及请求状态继续由 email Feature 与既有 ViewModel/Paging 持有；main 空详情占位复用主题标题栏。无新增模块、依赖或数据库迁移。保持卡片、正文宽度、导航与分栏规则，正式截图基线不更新。

## 验证证据

- 邮件模块 23 项、data 15 项、theme 7 项、About 30 项宿主测试通过，无失败或跳过。
- 提交前组合根筛选回归 30 项：14 通过、16 条件跳过，无失败；包括双栏空态对齐与既有邮件场景测试。
- 完成提示新增断言覆盖末页、空列表、仍有下一页、刷新中及带真实 FAB 和底部安全距离时的最终间距。失败重试用例同步检查不误显示完成提示。最终间距断言确认提示距安全区内底边仅 8dp。
- 手势回归通过真实 Pager 验证空收藏列表下拉后重新加载。独立 SDK 36 沙箱避免 Paging 缓存 UI 调度器与其他用例的帧时钟交叉；其他邮件布局回归使用 SDK 37。
- Android 整包构建与 iOS 模拟器 Kotlin 编译通过。git diff --check 与文档本地文件链接检查通过。
- 四模块 detekt 已执行，仍被未改动声明的既有告警阻断；本次新增格式与复杂条件告警已修复，不生成 baseline。email 剩余告警来自 NewEmailSheet、EmailDetailScreen 与 EmailSearchBar；其他模块保留原有格式、复杂度及 Compose Rules 告警。

验证入口：按序执行 ./gradlew :feature:email:testAndroidHostTest :core:data:testAndroidHostTest :core:theme:testAndroidHostTest :feature:about:testAndroidHostTest，组合根筛选 HalfOpenPaneTest 与邮件相关测试；构建执行 :androidApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64。构建使用命令级 JAVA_HOME，不改系统配置。

未采集候选截图、未更新正式基线、未修改模拟器显示设置。双端设备、大字号与连续动画仍待验收，见[待验收清单](../../testing/pending-acceptance.md#邮件刷新与详情布局2026-11-20)。
