# 项目指令（BJXD每日任务）

## 通用

- 本文件是本仓库的 agent 指令；任何 UI/UX 改动前必须遵守下方"UI 设计规范"。
- 审计证据与历史报告见 `audit/<日期>/`（当前 `audit/2026-11-24/`）。

## UI 设计规范（原 DESIGN_RULES.md，已合并至此）

- 技术栈：Jetpack Compose BOM 2026.09.00 + Material 3 Expressive，AGP 9.5.1 / Gradle 9.9.1 / Kotlin
  2.2.10；compileSdk 37 / targetSdk 37 / minSdk 25。
- 美学锚点： **Material You**（动态取色、expressive 形状/动效）。全部设计选择必须能追溯到该锚点。

### 启用的禁令

- **Ban 1** — 禁止通用渐变（紫→粉、多色 brush）。
- **Ban 2** — 禁止占位/水文案（Lorem ipsum、Powered by AI、泛化 CTA）。
- **Ban 3** — 禁止任意数值：dp/sp/Color 全部来自主题 token。
- **Ban 4** — 禁止默认字体痕迹（不要 `FontFamily("Inter")`，用 `FontFamily.Default`）。
- **Ban 6** — 禁止 Card 嵌套 Card、混用 elevation。
- **Ban 7** — 禁止通用布局套路（三卡 features 网格、居中 hero+双按钮）。
- **Ban 8** — 禁止装饰性无限动画（`rememberInfiniteTransition`、自动轮播）。

Ban 5（毛玻璃）未启用，但 Material You 不声明 blur，因此不用 blur。

### Token 单一数据源

| Token                | 文件                                                       |
|----------------------|------------------------------------------------------------|
| ColorScheme（明/暗） | `app/src/main/kotlin/com/guyuexuan/bjxd/ui/theme/Color.kt` |
| Typography           | `.../ui/theme/Type.kt`                                     |
| Shapes               | `.../ui/theme/Shape.kt`                                    |
| Spacing              | `.../ui/theme/Spacing.kt`                                  |
| Theme wrapper        | `.../ui/theme/Theme.kt`                                    |
| Motion               | Material 3 MotionTokens（无自定义文件）                    |

**不得使用 token 之外的值**：禁止 `Modifier.padding(13.dp)`、内联 `Color(0xFF...)`、
`TextStyle(fontSize = 13.sp)`、一次性 `RoundedCornerShape(7.dp)`。需要新值时先要求扩展主题。

### 项目级选择

- Card 变体：`Card`（M3 filled），不嵌套。
- 按压反馈：ripple（M3 组件默认）。
- 密度：balanced；深色主题：必须（两套色都填）；动态取色：开（API 31+，静态种子 #6750A4 兜底）。
- 图标族：Material Symbols **Outlined**（选中态可用 Filled 作为唯一例外）。

### 验证命令

```
./gradlew checkDesignRules lint
```

`checkDesignRules` 走 `detektDesign`（基于正则的设计规则检查，因 detekt 1.x 与 Kotlin 2.2.10 不兼容而未接入真实
detekt 插件）。`config/detekt/detekt.yml` 为参考文档，实际规则在 `app/build.gradle`。

### 参考

- 组件尺寸规范：`~/.agents/skills/ui-android/component-anatomy.md`（触控目标
  ≥48dp、edge-to-edge、predictive back）。
- 文案规范：`~/.agents/skills/ui-android/copy-voice.md`（中文文案要具体，无 lorem/filler）。
