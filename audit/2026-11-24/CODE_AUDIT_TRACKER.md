# Code Audit Tracker

- Audit run: AUDIT-2026-11-24-01
- Last updated: 2026-11-24 (after remediation)
- Overall status: COMPLETE_WITH_LIMITATIONS

## Coverage summary

| Domain                         | Applicable files | Reviewed | Findings |                 Blocked / unreviewed | Status                    |
|--------------------------------|-----------------:|---------:|---------:|-------------------------------------:|---------------------------|
| D01 Kotlin correctness         |               12 |       12 |        3 |                                    0 | COMPLETE_WITH_FINDINGS    |
| D02 Compose UI/state           |               17 |       15 |        4 | 2 (theme token files not line-level) | COMPLETE_WITH_LIMITATIONS |
| D03 Lifecycle/navigation       |               12 |       12 |        1 |                                    0 | COMPLETE_WITH_FINDINGS    |
| D04 Coroutines/concurrency     |               10 |       10 |        4 |                                    0 | COMPLETE_WITH_FINDINGS    |
| D05 Architecture/DI            |                8 |        8 |        1 |                                    0 | COMPLETE_WITH_FINDINGS    |
| D06 Persistence                |                8 |        8 |        3 |                                    0 | COMPLETE_WITH_FINDINGS    |
| D07 Networking                 |                6 |        6 |        4 |                                    0 | COMPLETE_WITH_FINDINGS    |
| D08 Security/privacy           |               14 |       14 |        4 |                                    0 | COMPLETE_WITH_FINDINGS    |
| D09 Performance                |                8 |        8 |        4 |                 0 (profiler not run) | COMPLETE_WITH_LIMITATIONS |
| D10 Accessibility/adaptive     |               15 |       13 |        3 |                  2 (device/TalkBack) | COMPLETE_WITH_LIMITATIONS |
| D11 Testing/build/release      |                9 |        9 |        7 |                                    0 | COMPLETE_WITH_FINDINGS    |
| D12 Observability/dependencies |                9 |        9 |        5 |                                    0 | COMPLETE_WITH_FINDINGS    |

## Findings register (deduplicated; post-remediation status)

| ID  | Severity | Confidence                   | Summary                 | Status                     | Evidence of fix / decision                                                                                                                                          |
|-----|----------|------------------------------|-------------------------|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| F01 | Medium   | Confirmed                    | JSON 空安全             | FIXED_UNVERIFIED           | `ApiUtil.parseObject/required/readMsg`; `User.fromJson` 校验；`askAI` choices 判空；4 单测                                                                          |
| F02 | Medium   | Confirmed                    | 吞 InterruptedException | VERIFIED_FIXED (static)    | `TaskRunner` 先 catch 并 re-interrupt 再抛出                                                                                                                        |
| F03 | Medium   | Probable                     | runner 可见性           | VERIFIED_FIXED (static)    | `TaskService.runner` 声明 volatile                                                                                                                                  |
| F04 | Medium   | Confirmed                    | 同步页旋转丢状态        | FIXED_UNVERIFIED           | `SyncStateStore` 进程级状态；SyncActivity 只读该 store；无设备旋转测试                                                                                              |
| F05 | Medium   | Confirmed                    | token 重复并发请求      | FIXED_UNVERIFIED           | `inFlightToken` + 5 秒失败节流；无注入延迟测试                                                                                                                      |
| F06 | High     | Probable                     | 明文/secret query       | FIXED_UNVERIFIED (partial) | AI URL 强制 HTTPS + 青龙 HTTP 风险提示 + HttpUrl 编码；base-config cleartext 仍 true（内网青龙需要），客户端 secret 签名不改（青龙 API 为 GET，改 POST 会破坏功能） |
| F07 | Medium   | Confirmed                    | 取消不到网络层          | FIXED_UNVERIFIED           | `ApiUtil.cancelAllCalls()` 在停止/超时时调用 dispatcher.cancelAll                                                                                                   |
| F08 | Low      | Confirmed                    | retry false 无延迟      | VERIFIED_FIXED (static)    | `retrySend` 统一失败延迟；create 幂等未改（改动大，边界复杂）                                                                                                       |
| F09 | Low      | Confirmed                    | URL 编码/parse 判空     | VERIFIED_FIXED (static)    | `HttpUrl.parse` 判空 + addQueryParameter                                                                                                                            |
| F10 | Low      | Needs validation             | 壁钟超时                | VERIFIED_FIXED (static)    | `SystemClock.elapsedRealtime()`                                                                                                                                     |
| F11 | High     | Confirmed                    | 零可执行测试            | FIXED_UNVERIFIED           | 新增 `ModelLogicTest` 4 个 JVM 单测（tests=4, failures=0, skipped=0）；完整 HTTP/协程测试仍未建                                                                     |
| F12 | Medium   | Probable                     | WebView token 信任      | ACCEPTED_RISK              | 前提高（需控制登录页）；hid 本地匹配省接口调用是用户明确需求；WebClient 白名单可能破坏登录跳转链；无 addJavascriptInterface/SSL 忽略                                |
| F13 | Low      | Confirmed                    | 强制滚到底              | FIXED_UNVERIFIED           | `LogView` 仅底部附近跟随（用 toPx 阈值）                                                                                                                            |
| F14 | Medium   | Confirmed                    | 文案硬编码              | ACCEPTED_RISK              | 中文个人工具无本地化需求；全量迁移 stringResource 收益低、风险高                                                                                                    |
| F15 | Low      | Confirmed                    | remember 丢状态         | ACCEPTED_RISK              | 敏感输入不应写 Bundle；其余状态可从存储恢复                                                                                                                         |
| F16 | Low      | Needs validation             | RadioButton 语义        | FIXED_UNVERIFIED           | `Modifier.semantics { contentDescription = "选项 X" }`；TalkBack 未实测                                                                                             |
| F17 | Low      | Needs validation             | 日志 O(n²)              | ACCEPTED_RISK              | 影响未测量；需先 profiling；改动架构风险大于收益                                                                                                                    |
| F18 | Medium   | Confirmed                    | 明文密钥                | ACCEPTED_RISK              | 前提需 root/取证；MODE_PRIVATE + 备份排除已有基本保护；加密迁移风险大于收益                                                                                         |
| F19 | Medium   | Confirmed                    | 日志备份遗漏            | VERIFIED_FIXED (static)    | 两份 XML 均排除 `file/last_task_log.txt`                                                                                                                            |
| F20 | Low      | Probable                     | AI Key 任意 URL         | FIXED_UNVERIFIED           | AI URL 强制 HTTPS；青龙保留 HTTP 但提示风险                                                                                                                         |
| F21 | Medium   | Confirmed                    | 发布加固                | FIXED_UNVERIFIED (partial) | 移除弱口令默认；缺属性回退 AGP 默认 debug 签名；minify 未启用（Gson 反射风险）                                                                                      |
| F22 | Low      | Probable                     | apply() 丢写            | VERIFIED_FIXED (static)    | `saveUserList` 改用 commit()；错误 JSON 由 `User.fromJson` 抛明确异常                                                                                               |
| F23 | Info     | Confirmed                    | 依赖校验                | ACCEPTED_RISK              | Info 级；镜像+代理环境下生成 verification-metadata 收益低                                                                                                           |
| F24 | Medium   | Confirmed/Probable           | LazyColumn 缺 key       | VERIFIED_FIXED (static)    | `itemsIndexed(..., key = phone, contentType = "account")`                                                                                                           |
| F25 | Low      | Confirmed                    | 1 秒轮询                | FIXED_UNVERIFIED           | 改为 `TaskLogStore` listener 事件驱动                                                                                                                               |
| F26 | Medium   | Confirmed                    | detekt 别名             | VERIFIED_FIXED (static)    | 改名 `checkDesignRules`，DESIGN_RULES 同步                                                                                                                          |
| F27 | Low      | Confirmed                    | returnDefaultValues     | VERIFIED_FIXED (static)    | 设 false；新单测以 Okio 解码避免 Android stub                                                                                                                       |
| F28 | Medium   | Confirmed                    | 无 CI                   | ACCEPTED_RISK              | 个人仓库无 CI 基建；本地 `detekt/lint/test` 已可覆盖                                                                                                                |
| F29 | Medium   | Probable                     | FGS 6h 限额             | FIXED_UNVERIFIED           | 覆盖 `onTimeout()` 优雅收尾；平台时限未实测                                                                                                                         |
| F30 | High     | Confirmed / Needs validation | git 历史凭据            | OPEN (用户运维)            | 需用户轮换青龙 secret/token 并决定是否重写历史；agent 未连接外部服务                                                                                                |
| F31 | Low      | Confirmed                    | 未用依赖                | VERIFIED_FIXED (static)    | 移除 appcompat/constraintlayout/recyclerview/webkit/lifecycle bundle；编译通过                                                                                      |
| F32 | Low      | Needs validation             | 通知锁屏可见性          | FIXED_UNVERIFIED           | `VISIBILITY_PRIVATE` + publicVersion                                                                                                                                |
| F33 | Low      | Confirmed                    | 进程死亡不恢复          | ACCEPTED_RISK              | `START_NOT_STICKY` 避免重复执行；日志已落盘可查；产品决策                                                                                                           |

## Decisions / accepted risks

| ID                          | Decision   | Rationale                                                                |
|-----------------------------|------------|--------------------------------------------------------------------------|
| F12                         | 保持现状   | 攻击前提高；hid 匹配省一次接口调用是用户明确需求；白名单可能破坏登录跳转 |
| F14,F15,F17,F18,F23,F28,F33 | 不修       | 收益低于改动/迁移风险，或无本地化/CI/加密需求；详见各条理由              |
| F30                         | 需用户处理 | 凭据轮换与 git 历史重写需用户授权，agent 不连接外部服务、不 force push   |

## Audit history

| Date       | Run                               | Scope                                 | Result                    | Report                    |
|------------|-----------------------------------|---------------------------------------|---------------------------|---------------------------|
| 2026-11-24 | AUDIT-2026-11-24-01 (initial)     | full, c51b87c                         | COMPLETE_WITH_LIMITATIONS | CODE_AUDIT_REPORT.md      |
| 2026-11-24 | AUDIT-2026-11-24-01 (remediation) | 24 findings, one root cause at a time | COMPLETE_WITH_LIMITATIONS | REGRESSION_TEST_REPORT.md |
