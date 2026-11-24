# Final Android Compose Audit Report — AUDIT-2026-11-24-01

- Date: 2026-11-24
- Repository / branch / commit: `BJXD-AutoTask-Android` / `main` /
  `c51b87cc5356d76cc4e6bb2f4f3c9df7cca3f15b`
- Mode: `full` (12 domains, single `:app` module)
- Overall status: **COMPLETE_WITH_LIMITATIONS**

## Executive summary

- Modules: 1 (`:app` application)
- In-scope files reviewed: all `app/src/main` Java/Kotlin sources, Manifest, res/xml, values themes,
  Gradle build logic, tests, git config/history (see `PROJECT_INVENTORY.md`)
- Domains completed: 12/12 (some with explicit limitations)
- Findings: **Critical 0 / High 3 / Medium 16 / Low 12 / Info 1** (32 total, deduplicated from 4
  independent audit passes)
- Confidence: Confirmed 24 / Probable 5 / Needs validation 3 (per tracker; some findings carry mixed
  labels)
- No source code was modified in this run (skill rule: discovery is read-only)

## Scope and exclusions

Full project: single application module, debug/release build types, no flavors, no dynamic features.
Excluded: generated `build/` and `.gradle/`, IDE files, binary assets, gitignored
keystore/APK/local.properties. Rationale in `PROJECT_INVENTORY.md`.

## Findings by priority

See `CODE_AUDIT_REPORT.md` and `CODE_AUDIT_TRACKER.md`. Top three:

| ID  | Severity | Confidence                   | Summary                                                      |
|-----|----------|------------------------------|--------------------------------------------------------------|
| F06 | High     | Probable                     | 青龙 secret 走 GET query + 全局明文放行 + URL 无 scheme 校验 |
| F11 | High     | Confirmed                    | 零可执行测试（10/10 skipped，无 UI 测试）                    |
| F30 | High     | Confirmed / Needs validation | git 历史残留疑似真实青龙凭据                                 |

Medium highlights: F01 (JSON 空安全), F04 (同步页旋转丢状态), F07 (取消不到网络层), F12 (WebView
token 信任), F18 (明文密钥存储), F19 (日志备份遗漏), F24 (LazyColumn 缺 key), F26 (detekt 别名), F29
(FGS 6h 限额).

## Coverage

| Domain                         | Reviewed / applicable | Status                    | Report                   |
|--------------------------------|----------------------:|---------------------------|--------------------------|
| D01 Kotlin correctness         |                 12/12 | COMPLETE_WITH_FINDINGS    | CODE_AUDIT_REPORT.md     |
| D02 Compose UI/state           |                 15/17 | COMPLETE_WITH_LIMITATIONS | CODE_AUDIT_REPORT.md     |
| D03 Lifecycle/navigation       |                 12/12 | COMPLETE_WITH_FINDINGS    | CODE_AUDIT_REPORT.md     |
| D04 Coroutines/concurrency     |                 10/10 | COMPLETE_WITH_FINDINGS    | CODE_AUDIT_REPORT.md     |
| D05 Architecture/DI            |                   8/8 | COMPLETE_WITH_FINDINGS    | CODE_AUDIT_REPORT.md     |
| D06 Persistence                |                   8/8 | COMPLETE_WITH_FINDINGS    | CODE_AUDIT_REPORT.md     |
| D07 Networking                 |                   6/6 | COMPLETE_WITH_FINDINGS    | CODE_AUDIT_REPORT.md     |
| D08 Security/privacy           |                 14/14 | COMPLETE_WITH_FINDINGS    | SECURITY_AUDIT_REPORT.md |
| D09 Performance                |                   8/8 | COMPLETE_WITH_LIMITATIONS | CODE_AUDIT_REPORT.md     |
| D10 Accessibility/adaptive     |                 13/15 | COMPLETE_WITH_LIMITATIONS | CODE_AUDIT_REPORT.md     |
| D11 Testing/build/release      |                   9/9 | COMPLETE_WITH_FINDINGS    | CODE_AUDIT_REPORT.md     |
| D12 Observability/dependencies |                   9/9 | COMPLETE_WITH_FINDINGS    | CODE_AUDIT_REPORT.md     |

## Cross-cutting flows traced

| Flow               | Entry                                    | State/data path                        | Failure/lifecycle paths                           | Tests      |
|--------------------|------------------------------------------|----------------------------------------|---------------------------------------------------|------------|
| 启动 → 主页        | MainActivity.onCreate                    | StorageUtil → mutableStateListOf       | onResume 刷新；进程死亡由存储恢复                 | none (F11) |
| 手机号登录自动添加 | WebView localStorage → onDetectedToken   | JWT hid → StorageUtil → ActivityResult | 失败重复请求(F05)；伪造 token 覆盖(F12)           | none       |
| 任务执行           | FAB → TaskService → TaskRunner → ApiUtil | TaskLogStore → UI listener → 通知      | 取消不到网络层(F07)；停止被吞(F02)；FGS 超时(F29) | none       |
| 青龙双向同步       | 首页确认 → SyncActivity → QinglongSync   | 本地 prefs ↔ 青龙 envs                 | 旋转丢状态(F04)；重试无退避(F08)；进程死亡(F33)   | none       |
| 答题               | 通知 → AnswerActivity → submitAnswer     | answerLock → TaskRunner                | 壁钟超时(F10)；语义缺失(F16)                      | none       |

## Commands and validation

| Command                                                                | Exit/result                 | Notes                              |
|------------------------------------------------------------------------|-----------------------------|------------------------------------|
| `./gradlew detekt lint test assembleDebug --no-daemon --console=plain` | BUILD SUCCESSFUL (60 tasks) | audit-run baseline                 |
| `git rev-parse HEAD`                                                   | c51b87c…                    | baseline commit                    |
| `git status --porcelain`                                               | clean at start              | audit artifacts created afterwards |

Domain agents performed read-only inspection only and did not execute Gradle tasks; the build result
above is the audit-run baseline.

## Regression status (post-remediation)

See `REGRESSION_TEST_REPORT.md`. 24 findings were re-evaluated against the user's four questions (
"exists? harmful? benefit to fix? harm if not?") and remediated where justified:

- **Verified fixed (static/unit)**: F02, F03, F08, F09, F10, F19, F22, F24, F26, F27, F31 — plus 4
  new JVM unit tests (`ModelLogicTest`, tests=4/failures=0/skipped=0) partially covering F01.
- **Fixed but unverified on device**: F01, F04, F05, F06 (partial), F07, F11 (partial), F13, F16,
  F20, F21 (partial), F25, F29, F32.
- **Accepted risk with rationale**: F12, F14, F15, F17, F18, F23, F28, F33.
- **Open, requires user action**: F30 (credential rotation / history rewrite).

Verification after remediation: `./gradlew detekt lint test assembleDebug --no-daemon` → BUILD
SUCCESSFUL (60 tasks); independent closeout found no new defects from the patch.

## Limitations and residual risks

- 无真机/设备运行时验证：WebView 白名单、TalkBack 语义、锁屏通知、FGS 6 小时超时、备份归档、进程死亡。
- 未做 profiling / Macrobenchmark；D09 性能项均为静态观察。
- 未执行 release/minified 构建；R8 对 Gson 模型的影响未验证。
- 后端行为（北京现代 API、百度、智谱、青龙）不在仓库证据内，相关结论均写明前提。
- 未验证 git 历史凭据是否仍有效（未连接外部服务）。
- `./gradlew test` 虽通过但 10/10 skipped，不构成行为回归证据。
- 主题 token 文件（Color/Type/Shape/Spacing）仅确认存在与引用，未逐行审计。

## Conclusion

**COMPLETE_WITH_LIMITATIONS**: 12
个领域审计、跨流程追踪、确定性检查、发现去重定级、以及经用户授权后的修复与独立回归复核均已完成。剩余限制：设备/运行时/性能/发布构建未验证；F30
需用户运维；8 项发现被显式接受为风险（理由见 tracker）。本报告不构成"项目无缺陷"的保证。
