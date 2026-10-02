# iOS Toast 无障碍修复计划

- [x] 将现有 ToastViewController 的可见性改为 internal，便于模块内原生测试检查真实 UILabel（不改变行为）。
- [x] 添加字体配置回归测试并确认固定字体实现失败。
- [x] 接入 Dynamic Type、字号变化布局通知及淡入后的无障碍播报；保持取消与窗口关闭清理。
- [x] 运行 core:framework iOS 测试和 composeApp iOS 编译。
- [x] 独立审查生命周期，未发现阻塞问题。
- [x] 更新架构与审计状态，检查最终差异。
