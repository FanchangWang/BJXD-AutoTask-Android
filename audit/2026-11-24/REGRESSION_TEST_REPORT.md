# Regression Test Report — AUDIT-2026-11-24-01

- Audit run: AUDIT-2026-11-24-01 (remediation pass)
- Baseline commit: c51b87cc5356d76cc4e6bb2f4f3c9df7cca3f15b
- Current state: working tree after fixes (not yet committed at time of writing)
- Build variant: debug
- Test environment: local JDK 21 / Gradle 9.9.1 / Android 14 emulator available; no device test
  executed in this pass

## Prior findings rechecked and fixed

| ID  | Original failure condition | Reproduction / inspection                                   | Result                                           | Status                                     |
|-----|----------------------------|-------------------------------------------------------------|--------------------------------------------------|--------------------------------------------|
| F01 | 缺字段 JSON 导致 NPE       | `parseObject/required` 代码复查 + `User.fromJson` 单测 2 项 | 空/畸形响应抛明确 IOException；单测通过          | VERIFIED_FIXED (static + unit)             |
| F02 | 停止信号被吞               | 复查 `executeUserTask` catch 顺序                           | InterruptedException 先 re-interrupt 再抛出      | VERIFIED_FIXED (static)                    |
| F03 | 跨线程可见性               | 复查字段声明                                                | `volatile TaskRunner runner`                     | VERIFIED_FIXED (static)                    |
| F04 | 旋转丢同步状态             | 复查 SyncActivity + SyncStateStore                          | 状态改为进程级 store；无设备旋转测试             | FIXED_UNVERIFIED                           |
| F05 | token 重复请求             | 复查 `onDetectedToken`                                      | inFlightToken + 5s 失败节流；无注入延迟测试      | FIXED_UNVERIFIED                           |
| F06 | secret query/明文          | 复查 ConfigActivity/ApiUtil/NetworkConfig                   | AI URL 强制 HTTPS；青龙 HTTP 提示；HttpUrl 编码  | FIXED_UNVERIFIED (partial)                 |
| F07 | 在途请求无法取消           | 复查 stopCurrentTask/onTimeout                              | `ApiUtil.cancelAllCalls()`                       | FIXED_UNVERIFIED                           |
| F08 | retry false 无退避         | 复查 retrySend                                              | 统一失败延迟                                     | VERIFIED_FIXED (static)                    |
| F09 | 参数未编码                 | 复查 getQinglongEnvList/Auth                                | HttpUrl + parse 判空                             | VERIFIED_FIXED (static)                    |
| F10 | 壁钟超时                   | 复查 requestManualAnswer                                    | elapsedRealtime                                  | VERIFIED_FIXED (static)                    |
| F11 | 零可执行测试               | `./gradlew test` + XML                                      | 新增 4 个 JVM 单测：tests=4 failures=0 skipped=0 | FIXED_UNVERIFIED（完整 HTTP/协程测试未建） |
| F13 | 日志强制滚到底             | 复查 LogView                                                | 仅在底部阈值内跟随                               | FIXED_UNVERIFIED                           |
| F16 | 选项语义缺失               | 复查 AnswerScreen                                           | RadioButton 加 contentDescription                | FIXED_UNVERIFIED (TalkBack 未实测)         |
| F19 | 日志被备份                 | 复查两份 XML                                                | 均排除 file/last_task_log.txt                    | VERIFIED_FIXED (static)                    |
| F20 | AI Key 任意 URL            | 复查 ConfigActivity.save                                    | HTTPS 校验                                       | FIXED_UNVERIFIED                           |
| F21 | 弱口令回退                 | 复查 app/build.gradle                                       | 无默认口令；缺属性回退默认 debug 签名            | FIXED_UNVERIFIED (release 未验证)          |
| F22 | apply() 丢写               | 复查 StorageUtil.saveUserList                               | commit()                                         | VERIFIED_FIXED (static)                    |
| F24 | LazyColumn 缺 key          | 复查 MainScreen                                             | key=phone, contentType                           | VERIFIED_FIXED (static)                    |
| F25 | 每秒轮询                   | 复查 MainActivity                                           | TaskLogStore listener 事件驱动                   | FIXED_UNVERIFIED                           |
| F26 | detekt 别名                | `./gradlew checkDesignRules`                                | 任务改名 + 文档同步                              | VERIFIED_FIXED (static)                    |
| F27 | returnDefaultValues        | 复查 app/build.gradle + test                                | false；单测通过                                  | VERIFIED_FIXED (static)                    |
| F29 | FGS 6h 限额                | 复查 TaskService.onTimeout                                  | 覆盖 onTimeout 优雅收尾                          | FIXED_UNVERIFIED                           |
| F31 | 未用依赖                   | 编译 + lint                                                 | 移除 5 组依赖后构建通过                          | VERIFIED_FIXED (static)                    |
| F32 | 通知锁屏可见性             | 复查 TaskService                                            | VISIBILITY_PRIVATE + publicVersion               | FIXED_UNVERIFIED                           |

## New findings introduced by the remediation

| ID | Severity | Evidence                       | Impact |
|----|----------|--------------------------------|--------|
| —  | —        | 独立复核未发现新增缺陷（见下） | —      |

## Commands

| Command                                                                | Exit code | Result                                                                            |
|------------------------------------------------------------------------|----------:|-----------------------------------------------------------------------------------|
| `./gradlew detekt lint test assembleDebug --no-daemon --console=plain` |         0 | BUILD SUCCESSFUL, 60 tasks                                                        |
| `./gradlew compileDebugKotlin`（修复过程中）                           |         0 | 修复了 2 处 Compose 编译错误后通过                                                |
| `Get-ChildItem app/build/test-results/testDebugUnitTest/TEST-*.xml`    |         0 | ModelLogicTest: tests=4 failures=0 skipped=0; ApiUtilTest: 10 skipped（手工测试） |

## Changed-file impact review

| Changed file                                                             | Callers/callees reviewed                                | Related flows        | Tests                |
|--------------------------------------------------------------------------|---------------------------------------------------------|----------------------|----------------------|
| ApiUtil.java (parse helpers, cancelAll, HttpUrl)                         | 全量 17 处 newCall 调用点；QinglongSync/AddUserActivity | 登录、任务、同步、AI | ModelLogicTest(部分) |
| User.java (JWT via Okio, fromJson 校验)                                  | ApiUtil.getUserInfo、AddUserActivity、QinglongSync      | 登录/同步            | 4 项单测             |
| TaskService.java (volatile, elapsedRealtime, cancelAll, onTimeout, 通知) | MainActivity、TaskActivity、TaskActionReceiver          | 任务/答题/通知       | 静态复查             |
| TaskRunner.java (InterruptedException)                                   | TaskService、ApiUtil                                    | 停止任务             | 静态复查             |
| QinglongSync.kt (retry 延迟)                                             | SyncActivity                                            | 同步                 | 静态复查             |
| SyncActivity.kt / SyncStateStore.kt                                      | MainScreen 同步入口                                     | 同步/旋转            | 静态复查             |
| AddUserActivity.kt (inFlight)                                            | AddUserScreen 轮询                                      | 登录自动添加         | 静态复查             |
| MainActivity.kt (listener)                                               | TaskLogStore、TaskService                               | 首页 FAB 状态        | 静态复查             |
| MainScreen.kt / LogView.kt / AnswerScreen.kt                             | 各 Activity                                             | 列表/日志/答题       | 静态复查             |
| ConfigActivity.kt (HTTPS 校验)                                           | StorageUtil                                             | 配置保存             | 静态复查             |
| app/build.gradle / DESIGN_RULES.md / backup XML / deps                   | 全局构建                                                | 构建/备份            | 编译验证             |

## Independent closeout check

按 skill Phase 7 以"另一个工程师的视角"重开改动代码复核：

- 原失败条件：逐条对照上表，代码层面均已消除或降级为 ACCEPTED_RISK。
- 调用方/共享状态：SyncStateStore 为新增共享源，SyncActivity 所有读写均经它；MainActivity 改 listener
  后不再轮询；ApiUtil.cancelAllCalls 在停止与超时两处调用。
- 生命周期/取消：SyncActivity scope 仍故意不取消（作业需跑完），但 UI 状态不再绑 Activity；onDestroy
  未新增泄漏点（旧协程仍持有 Activity 引用至同步结束，属 F04 残余风险，已标注 FIXED_UNVERIFIED）。
- 错误路径：`User.fromJson` 抛 IllegalArgumentException 由 AddUserActivity 的 catch (Exception)
  捕获并展示错误；`parseObject` 抛 IOException 由各调用方 catch 处理。
- 构建/lint/测试：全绿。
- 未发现由本次补丁引入的新缺陷。

## Remaining gaps

- 设备验证：旋转恢复、WebView 白名单、TalkBack、锁屏通知、FGS 超时、备份归档、进程死亡。
- F30 需用户运维（轮换凭据 + 决定是否重写历史）。
- F12/F14/F15/F17/F18/F23/F28/F33 为 ACCEPTED_RISK，详见 tracker。
