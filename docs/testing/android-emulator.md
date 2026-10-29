# Android 模拟器多形态测试

本文记录本项目进行尺寸、旋转和折叠姿态验证时的操作约束。架构判据见 [项目架构](../architecture.md)。

## 1. 选择设备

先确认设备序列号，后续每条命令都显式使用 `-s`，避免改到其他模拟器或真机：

```bash
adb devices -l
adb -s emulator-5554 shell getprop ro.build.version.sdk
```

将示例中的 `emulator-5554` 替换为实际序列号。

## 2. 必须注册退出清理

任何修改分辨率、显示密度或旋转状态的脚本，都必须在修改前注册清理函数。即使测试失败或被中断，也要恢复设备默认值。

```bash
device_serial=emulator-5554

cleanup_emulator_display() {
  adb -s "$device_serial" shell wm size reset
  adb -s "$device_serial" shell wm density reset
  adb -s "$device_serial" shell settings put system accelerometer_rotation 1
}

trap cleanup_emulator_display EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
```

交互式单条命令无法使用同一个 `trap` 时，必须在结束本次模拟器操作前手动执行同样的清理命令。

## 3. 修改尺寸和旋转

```bash
adb -s "$device_serial" shell wm size 1080x1920
adb -s "$device_serial" shell wm density 320

adb -s "$device_serial" shell settings put system accelerometer_rotation 0
adb -s "$device_serial" shell settings put system user_rotation 1
```

`user_rotation` 常用值为 `0`（自然方向）和 `1`（旋转 90°）。不要把某台 AVD 的物理分辨率或密度硬编码为“默认值”；恢复时始终使用 `reset`。

## 4. 读取应用实际窗口

`wm size` 和 `wm density` 描述显示器覆盖值，不能可靠地推导自由窗口或旋转后应用实际获得的 dp 尺寸。判断布局分支时，应读取应用窗口的 Configuration：

```bash
adb -s "$device_serial" shell dumpsys window windows \
  | grep -m1 -oE "w[0-9]+dp h[0-9]+dp [0-9]+dpi [a-z]+"
```

同时检查当前覆盖值：

```bash
adb -s "$device_serial" shell wm size
adb -s "$device_serial" shell wm density
```

## 5. 折叠姿态

先确认目标 AVD，再切换折叠状态。以下命令可直接在终端执行，将 `emulator-5554` 替换为实际序列号：

```bash
device_serial=emulator-5554
adb -s "$device_serial" emu avd name

# 折叠，切换到外屏
adb -s "$device_serial" emu fold

# 展开，切换到内屏
adb -s "$device_serial" emu unfold
```

需要半开等姿态时，先查看当前模拟器提供的姿态编号帮助，再分别执行目标命令：

```bash
adb -s "$device_serial" emu posture

# 闭合
adb -s "$device_serial" emu posture 1

# 半开
adb -s "$device_serial" emu posture 2

# 完全展开
adb -s "$device_serial" emu posture 3

# 支持铰链角度传感器的 AVD 可进一步设置为 90°
adb -s "$device_serial" emu sensor set hinge-angle0 90
```

以上是独立操作示例，不需要整段连续执行。`posture` 不带参数用于显示用法，可能返回 `KO usage`，不代表切换失败。常见 posture 值为 `1 closed`、`2 half-opened`、`3 opened`、`4 flipped`、`5 tent`，具体支持范围取决于 AVD；Pixel Fold 等标准模板通常只支持 `1`、`2`、`3`。半开姿态还需结合设备旋转验证书本式或桌面支架式布局。

结束测试时恢复测试前的折叠姿态；如果测试从完全展开开始，使用 `emu unfold` 恢复。折叠命令不设置 `wm size` / `wm density` 覆盖值；若测试还修改了尺寸、密度或旋转，仍须执行本文的退出清理和收尾验证。

命令依据见 [Android 官方模拟器控制说明](https://developer.android.com/blog/posts/emulator-control-for-adaptive-app-development)。模拟器注入不能替代物理折叠设备验证；修改窗口几何后，如果铰链不再横跨窗口，平台可能丢弃该 FoldingFeature。

布局验证应覆盖：

- 紧凑竖屏和紧凑横屏；
- Medium/Expanded 宽度；
- ExtraLarge 窗口；
- 书本式竖向铰链；
- 桌面支架式横向铰链；
- IME 展开时的输入页面。

## 6. 收尾验证

结束前执行清理函数，随后重新查询：

```bash
cleanup_emulator_display
trap - EXIT INT TERM

adb -s "$device_serial" shell wm size
adb -s "$device_serial" shell wm density
adb -s "$device_serial" shell settings get system accelerometer_rotation
```

合格结果应满足：

- `wm size` 只显示 `Physical size`，没有 `Override size`；
- `wm density` 只显示 `Physical density`，没有 `Override density`；
- `accelerometer_rotation` 为 `1`。

设备断连导致无法清理时，不得把任务标记为完成；应明确告知用户哪些设置尚未恢复。
