# Code Audit Report — AUDIT-2026-11-24-01

> Post-remediation note: the findings below are the original audit evidence. Their current
> disposition (FIXED_UNVERIFIED / VERIFIED_FIXED / ACCEPTED_RISK / OPEN) is tracked in
> `CODE_AUDIT_TRACKER.md`, with reproduction and verification details in `REGRESSION_TEST_REPORT.md`.

- Repository / branch / commit: `BJXD-AutoTask-Android` / `main` /
  `c51b87cc5356d76cc4e6bb2f4f3c9df7cca3f15b`
- Mode: `full` (12 domains)
- Auditor: OpenCode agent, skill `android-compose-code-audit`
- Status: COMPLETE_WITH_LIMITATIONS
- Scope: all `app/src/main` sources (Java + Kotlin), resources, manifests, Gradle build logic,
  tests, git config/history. Exclusions documented in `PROJECT_INVENTORY.md`.

## Findings by priority

| ID  | Severity | Confidence                   | Domain      | Summary                                                                                           |
|-----|----------|------------------------------|-------------|---------------------------------------------------------------------------------------------------|
| F06 | High     | Probable                     | D07,D08     | 全局明文放行 + 青龙 secret 走 URL query + 用户 URL 无 scheme 校验，可导致青龙凭据被同网段监听获取 |
| F11 | High     | Confirmed                    | D04,D10,D11 | 项目零可执行测试（唯一测试类整类 @Ignore，10/10 skipped），高风险业务无回归保护                   |
| F30 | High     | Confirmed / Needs validation | D12         | git 历史中残留疑似真实青龙凭据；HEAD 已清理但未轮换/未重写历史                                    |
| F01 | Medium   | Confirmed                    | D01,D07     | JSON 响应字段/空值未校验，Gson 平台类型直接解引用导致 NPE                                         |
| F02 | Medium   | Confirmed                    | D01,D04     | `executeUserTask` 宽泛 catch 吞掉 `InterruptedException`，停止延迟                                |
| F03 | Medium   | Probable                     | D01,D04     | `TaskService.runner` 非 volatile，跨线程可见性竞态                                                |
| F04 | Medium   | Confirmed                    | D03,D04,D09 | `SyncActivity` 旋转后丢日志/失败弹窗，旧协程持有已销毁 Activity                                   |
| F05 | Medium   | Confirmed                    | D04,D07     | token 检测失败后无在途去重/退避，每 1.5s 重复并发请求                                             |
| F07 | Medium   | Confirmed                    | D04,D07     | OkHttp `Call` 未持有/未 cancel，停止无法中断在途请求（AI 最长 5 分钟）                            |
| F12 | Medium   | Probable                     | D07,D08     | WebView 无 URL 白名单 + JWT `hid` 匹配即覆盖本地 token，跳过服务端校验                            |
| F14 | Medium   | Confirmed                    | D10         | 用户可见文案几乎全部硬编码                                                                        |
| F18 | Medium   | Confirmed                    | D06,D08     | Token/AI Key/百度 Key/青龙凭据明文存 SharedPreferences                                            |
| F19 | Medium   | Confirmed                    | D06,D08     | 任务日志文件未排除自动备份/设备迁移（上轮修复不完整）                                             |
| F21 | Medium   | Confirmed                    | D05,D11     | release 未签名/未混淆，debug 回退弱口令                                                           |
| F22 | Low      | Probable                     | D06         | `apply()` 异步落盘 + 损坏 JSON 无兜底                                                             |
| F24 | Medium   | Confirmed / Probable         | D09         | 账号 LazyColumn 无稳定 key，`animateItem` 与菜单状态可能错位                                      |
| F26 | Medium   | Confirmed                    | D11         | 伪装成 `detekt` 的别名任务，真实静态分析未接入                                                    |
| F28 | Medium   | Confirmed                    | D11         | 无 CI 流水线                                                                                      |
| F29 | Medium   | Probable                     | D11,D09     | targetSdk 37 `dataSync` FGS 6 小时限额未处理                                                      |
| F08 | Low      | Confirmed                    | D07,D12     | `retrySend` 返回 false 时无 5 秒退避；create 非幂等重试                                           |
| F09 | Low      | Confirmed                    | D01,D07     | 青龙列表接口参数未编码、`HttpUrl.parse` 未判空                                                    |
| F10 | Low      | Needs validation             | D01,D04     | 手动答题超时使用壁钟时间                                                                          |
| F13 | Low      | Confirmed                    | D02,D09     | LogView 强制滚到底，覆盖用户主动上滚                                                              |
| F15 | Low      | Confirmed                    | D02,D03     | 表单/对话框编辑态用 `remember`，旋转丢失                                                          |
| F16 | Low      | Needs validation             | D10         | AnswerScreen RadioButton 缺语义标签                                                               |
| F17 | Low      | Needs validation             | D09,D02     | 日志全量字符串 + 每行全量写文件，长日志 O(n²)                                                     |
| F20 | Low      | Probable                     | D08         | AI Key 可随用户自定义 URL 外发任意端点                                                            |
| F25 | Low      | Confirmed                    | D09,D12     | 首页每秒无限轮询任务状态                                                                          |
| F27 | Low      | Confirmed                    | D11         | `returnDefaultValues=true` 掩盖测试缺陷                                                           |
| F31 | Low      | Confirmed                    | D12         | 多个 XML 时代依赖未使用                                                                           |
| F32 | Low      | Needs validation             | D12         | 通知未设置锁屏可见性/公开兜底                                                                     |
| F33 | Low      | Confirmed                    | D12         | 进程死亡后任务不恢复、结果不补发                                                                  |
| F23 | Info     | Confirmed                    | D08,D12     | 未启用 Gradle 依赖校验/锁定                                                                       |

## High-severity detail

### F06 — 青龙凭据泄露路径（High, Probable）

- Evidence: `app/src/main/res/xml/network_security_config.xml:5` (`cleartextTrafficPermitted="true"`
  in `base-config`), `AndroidManifest.xml:22`, `ApiUtil.java:646-655` (`client_secret` 在 GET
  query), `ApiUtil.java:691-696`, `ConfigScreen.kt:252-258` (面板地址自由文本).
- Failure preconditions: 用户把青龙地址填为 `http://`（内网常见）并与攻击者处于同一网络路径；或 HTTPS 下
  secret 进入服务端/代理访问日志。
- Impact: CLIENT_ID/SECRET → `/open/auth/token` → `/open/envs` 可读取全部 `BJXD*` 账号 token。
- Recommendation: secret 改为 POST body/头；保存 URL 时强制 `https:`（内网 HTTP 需显式确认+局部
  `domain-config`）；移除 `base-config` 明文许可与 manifest 全局开关。
- Verification: 代理抓包确认 secret 不在 URL；`http://` 保存被拒绝；业务 HTTPS 域名禁止明文。

### F11 — 零可执行测试（High, Confirmed）

- Evidence: `ApiUtilTest.java:16-18` (整类 `@Ignore`),
  `app/build/test-results/.../TEST-...ApiUtilTest.xml:2` (`tests=10 skipped=10`), 无
  `app/src/androidTest` 源码。
- Impact: JWT 解析、青龙合并/重试、答案提取、0 分签到等全部高风险规则无回归保护；`./gradlew test`
  绿色但零执行。
- Recommendation: 为纯逻辑补 JVM 单测（`User.parseJwtLong/Hid`、`TaskRunner.extractAnswer`、注入 fake 的
  `QinglongSync`），引入 MockWebServer；把手工连通性脚本移出 `test` sourceSet。
- Verification: XML 报告 `tests > 0, skipped = 0`，每个上轮修复点有对应测试。

### F30 — git 历史凭据残留（High, Confirmed / Needs validation）

- Evidence: `git show 7d54d9b8` 测试文件中存在青龙 `client_secret` 与 token 字面量（报告已脱敏，未打印值）；HEAD
  `c51b87c` 仅保留占位符。
- Preconditions: 凭据仍有效且有人检出历史提交。
- Impact: 可直接访问用户青龙面板。
- Recommendation: **轮换**青龙 secret/token；评估 `git filter-repo`/BFG 清理历史并强制推送；加
  pre-commit 密钥扫描。
- Verification: 轮换后旧凭据鉴权失败；`git log -S` 不再命中 ASCII 凭据字面量。

## Checklist results by domain

### D01 Kotlin correctness — COMPLETE_WITH_FINDINGS

01 Nullability FINDING (F01) · 02 Exceptions FINDING (F02) · 03 Resource ownership PASS · 04
Collections PASS · 05 Concurrency FINDING (F03) · 06 API contracts FINDING (F09) · 07 Numeric/time
FINDING (F10) · 08 Serialization NEEDS-VALIDATION (F01) · 09 Complexity Info · 10 Logging PASS (附注：
`User.toString()` 含完整 token 但主链路未调用). Exit gate PASS.

### D02 Compose UI/state — COMPLETE_WITH_LIMITATIONS

01 State ownership PASS · 02 State writes PASS · 03 remember/rememberSaveable FINDING (F15) · 04
Effects PASS · 05 Lifecycle-aware collection N/A (no Flow) · 06 Events FINDING (F05) · 07 Stability
NEEDS-VALIDATION (F17) · 08 State restoration FINDING (F04,F15) · 09 Composable contracts PASS · 10
CompositionLocal PASS. Limitations: theme token 文件未逐行读取。

### D03 Lifecycle/navigation — COMPLETE_WITH_FINDINGS

01 Activity lifecycle FINDING (F04) · 02 ViewModel scope N/A (no ViewModel) · 03 Composable
lifecycle PASS · 04 Navigation PASS/N/A · 05 Back handling PASS (静态) · 06 State across navigation
FINDING (F04,F05) · 07 External intents PASS · 08 Configuration FINDING (F04,F15).

### D04 Coroutines/concurrency — COMPLETE_WITH_FINDINGS

01 Structured concurrency FINDING (F04) · 02 Dispatchers PASS · 03 Cancellation FINDING (F02,F07) ·
04/05 Flow N/A (no Flow) · 06 Races FINDING (F03,F05) · 07 Error handling FINDING (F05,F08) · 08
Deadlocks PASS · 09 Scope lifetime FINDING (F04) · 10 Tests FINDING (F11).

### D05 Architecture/DI — COMPLETE_WITH_FINDINGS

01 Layer boundaries PASS · 02 Business rules PASS · 03 Repository contract PASS · 04 Dependency
graph NEEDS-VALIDATION · 05 DI scopes NEEDS-VALIDATION · 06 Construction PASS · 07 Module API PASS ·
08 Build variants FINDING (F21) · 09 Non-dogmatic PASS.

### D06 Persistence — COMPLETE_WITH_FINDINGS

01 Room N/A · 02 Preferences FINDING (F22) · 03 Migration NEEDS-VALIDATION · 04 Cache PASS · 05
Serialization FINDING (F22) · 06 Integrity PASS · 07 Sensitive data FINDING (F18) · 08 Backup
FINDING (F19) · 09 File storage PASS · 10 Offline NEEDS-VALIDATION.

### D07 Networking — COMPLETE_WITH_FINDINGS

01 TLS/cleartext FINDING (F06) · 02 Auth NEEDS-VALIDATION (F12) · 03 Authorization N/A · 04 I/O
validation FINDING (F01,F09) · 05 Timeouts FINDING (F07) · 06 Retry FINDING (F08) · 07 Error mapping
NEEDS-VALIDATION · 08 Concurrency FINDING (F05) · 09 Logging PASS · 10 Network config FINDING (F06).

### D08 Security/privacy — COMPLETE_WITH_FINDINGS

01 Threat model PASS (本报告界定) · 02 Platform components PASS · 03 Auth/session NEEDS-VALIDATION
(F12,F18) · 04 Crypto PASS · 05 Local data FINDING (F18,F19) · 06 Network FINDING (F06,F09,F20) · 07
WebView FINDING (F12) · 08 Input/deep links NEEDS-VALIDATION · 09 Permissions PASS · 10 Supply chain
NEEDS-VALIDATION (F23) · 11 Release hardening FINDING (F19,F21) · 12 Privacy PASS (交叉引用) · 13
Preconditions PASS.

### D09 Performance — COMPLETE_WITH_LIMITATIONS

01 Main thread NEEDS-VALIDATION · 02 Compose phases NEEDS-VALIDATION · 03 Recomposition FINDING
(F17) · 04 Lazy layouts FINDING (F24) · 05 Images N/A · 06 Memory FINDING (F04) · 07 Startup
NEEDS-VALIDATION · 08 Battery FINDING (F25,F08,F29) · 09 ANR NEEDS-VALIDATION · 10 Measure-first
NEEDS-VALIDATION (no benchmark) · 11 Release FINDING (F21). 所有影响级结论均为静态观察，未测量。

### D10 Accessibility/adaptive — COMPLETE_WITH_LIMITATIONS

01 Semantics NEEDS-VALIDATION (F16) · 02 Touch PASS · 03 Visual NEEDS-VALIDATION · 04 Compose
semantics NEEDS-VALIDATION (F16) · 05 Localization FINDING (F14) · 06 Adaptive PASS (phone scope) ·
07 Text/input PASS · 08 Testing FINDING (F11). Limitations: TalkBack/设备验证未做。

### D11 Testing/build/release — COMPLETE_WITH_FINDINGS

01 Unit tests FINDING (F11) · 02 UI tests FINDING (F11) · 03 Integration FINDING (F11) · 04
Regression FINDING (F11) · 05 Test quality FINDING (F27) · 06 Coverage FINDING (F11) · 07 Build
NEEDS-VALIDATION (baseline pass) · 08 Compatibility FINDING (F29) · 09 Release config FINDING
(F21) · 10 CI FINDING (F28).

### D12 Observability/dependencies — COMPLETE_WITH_FINDINGS

01 Crash reporting NEEDS-VALIDATION (none) · 02 Logging FINDING (F19) · 03 Analytics N/A · 04
Dependencies FINDING (F31) · 05 Dependency management FINDING (F23) · 06 Background ops FINDING
(F08,F29) · 07 Notifications FINDING (F32) · 08 Operational failure FINDING (F33).

## Commands and validation (executed by this audit run, from repo root)

| Command                                                                | Result                                                       |
|------------------------------------------------------------------------|--------------------------------------------------------------|
| `git rev-parse HEAD`                                                   | `c51b87cc5356d76cc4e6bb2f4f3c9df7cca3f15b`                   |
| `git status --porcelain`                                               | clean at start (audit artifacts added later)                 |
| `./gradlew detekt lint test assembleDebug --no-daemon --console=plain` | BUILD SUCCESSFUL; 60 tasks; configuration cache entry reused |

Note: domain agents ran read-only inspections only; they did not execute Gradle tasks. The build
result above is the audit-run baseline, not a proof of runtime correctness.

## Cross-cutting flows traced

| Flow               | Entry                                                         | State/data path                        | Failure/lifecycle                                 | Tests |
|--------------------|---------------------------------------------------------------|----------------------------------------|---------------------------------------------------|-------|
| 启动 → 主页        | MainActivity.onCreate → reloadUsers → MainScreen              | StorageUtil → mutableStateListOf       | onResume 刷新；进程死亡由存储恢复                 | none  |
| 手机号登录自动添加 | WebView localStorage token → onDetectedToken → finishWithUser | JWT hid → StorageUtil → ActivityResult | 失败无退避(F05)；伪造 token 覆盖(F12)             | none  |
| 任务执行           | FAB → TaskService → TaskRunner → ApiUtil                      | TaskLogStore → UI listener → 通知      | 取消不到网络层(F07)；停止被吞(F02)；FGS 超时(F29) | none  |
| 青龙双向同步       | 首页二次确认 → SyncActivity → QinglongSync                    | 本地 prefs ↔ 青龙 envs                 | 旋转丢状态(F04)；重试无退避(F08)；进程死亡(F33)   | none  |
| 答题               | TaskService 通知 → AnswerActivity → submitAnswer              | answerLock → TaskRunner                | 壁钟超时(F10)；语义缺失(F16)                      | none  |

## Limitations

- 未做真机/设备运行时验证（WebView 白名单、TalkBack、锁屏通知、FGS 超时、备份归档、进程死亡）。
- 未做 profiling/Macrobenchmark，D09 性能项均为静态证据。
- 未执行 release/minified 构建，R8 对 Gson 模型的影响未验证。
- 后端行为（北京现代 API、百度、智谱、青龙）不在仓库证据内；相关结论均标注前提。
- 未验证 git 历史中的凭据是否仍然有效（不会连接外部服务）。

## Conclusion

COMPLETE_WITH_LIMITATIONS. 本轮在给定范围内完成 12
个领域审计并去重定级，但因设备/运行时/性能/发布构建验证缺失，不能声称项目无其他缺陷。按要求本轮未修改应用源码。
