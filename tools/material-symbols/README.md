# Material Symbols 图标来源与生成

应用内功能图标统一使用 Google Fonts Material Symbols Rounded，默认 `FILL=0`、`wght=400`、`GRAD=0`、`opsz=24`。`StarFill` 显式使用 `FILL=1`。品牌图标不在本工具范围内。

来源：[Google 官方 material-design-icons 仓库](https://github.com/google/material-design-icons)，固定提交 `737e3324305806514d7909874fa1818ae1808232`。`symbols/web/<name>/materialsymbolsrounded/<name>_24px.svg` 是未填充版本；`StarFill` 使用 `star_fill1_24px.svg`，例外记录在 manifest 的 `fill_overrides` 中。参数语义见 [Material Symbols 文档](https://developers.google.com/fonts/docs/material_symbols)。

`manifest.json` 保存公开 Kotlin 名称、官方符号名称、固定版本和每个 SVG 的 SHA-256。`Clear` 对应官方 `close`，其余沿用对应符号。原始 SVG 保存在 `svg/`，许可证见 `LICENSE`。SVG 的负 Y 视口转换为正 Y 视口：绝对坐标的 Y 值加 960，相对指令保持原值；路径不做手工改绘。输出沿用缓存属性和 `path { ... }` 指令，不使用 `addPathNodes`，填充为 `SolidColor(Color.Black)`，不输出原始注释，import 与正文之间保留两行空行。

在仓库根目录运行：

```bash
# 使用已保存的 SVG 重新生成，无需网络
python3 tools/material-symbols/generate.py

# 验证原始 SVG 校验值、生成内容和图标目录清单，无需网络
python3 tools/material-symbols/generate.py --check

# 验证绝对坐标、相对坐标及隐式指令转换
python3 -m unittest discover -s tools/material-symbols -p 'test_*.py'

# 重新下载固定版本的官方 SVG 并生成
python3 tools/material-symbols/generate.py --download
```

新增或升级图标时更新 manifest 并重新下载生成，再执行 `--check` 和 Android/iOS 编译。共享入口为 `Icons.Rounded`；保留 Star 和 StarFill，通过填充、按钮颜色和无障碍文案表达收藏状态。

项目指定的两行空行和原有 `_Name` 缓存属性命名与 detekt 默认格式规则不同，图标目录的这两类格式在模块 baseline 中记录；不改变其他文件的规则。

每个图标文件生成私有的 `<Name>Preview()`，使用 `@Preview` 和 `@Composable`，在 48dp 画布内居中显示图标。重新生成时保留预览方法；`Icons.kt` 只定义命名空间，不包含图标预览。
