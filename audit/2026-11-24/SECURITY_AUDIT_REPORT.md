# Security Audit Report — AUDIT-2026-11-24-01

- Scope: D08 security/privacy + D06 data-at-rest + D07 network boundaries + D12 operational security
- Commit: c51b87cc5356d76cc4e6bb2f4f3c9df7cca3f15b
- Status: COMPLETE_WITH_LIMITATIONS (静态证据；无真机/抓包/后端验证)

## Threat model (scope of conclusions)

- **Assets**: 用户账号 JWT（可直接调用北京现代业务 API）、AI/百度/青龙 API 密钥、青龙环境变量中的全量账号
  token、任务日志中的账号标识与行为数据。
- **Trust boundaries**: 登录 WebView 页面（外部内容）、用户可编辑的 AI/青龙 URL、业务 API
  与百度/智谱服务、青龙面板服务、Android 备份/设备迁移通道、git 历史。
- **Attackers considered**: 同网段被动观察者（rogue AP/ARP）；能影响 WebView 页面内容者；取得
  root/物理取证/备份访问能力者；能访问 git 历史者。
- **Not claimed**: 不凭代码模式断言远程可利用；所有发现均写明前提。

## Positive controls confirmed

- 所有非 MAIN 组件 `exported="false"`（Manifest:24-65）；`TaskActionReceiver` 不可被外部应用伪造广播。
- 所有 PendingIntent 使用 `FLAG_IMMUTABLE`（TaskService.java:255-333）；无 `addJavascriptInterface`
  ；WebView 未忽略 SSL 错误。
- 网络信任锚仅 `system` CA（network_security_config.xml:7,13），上一轮移除 user CA 有效。
- 生产源码无打印 token/密钥的 logcat 语句；手机号在 UI 与日志中打码（User.java:105-110,
  QinglongSync.kt:213-219）。
- `app_config.xml` 已从备份规则排除（backup_rules.xml:5、data_extraction_rules.xml:4,7）；keystore 未被
  git 跟踪。
- 权限集最小化且均被使用；`POST_NOTIFICATIONS` 运行时申请（MainActivity.kt:112-119）。

## Findings (security-relevant, deduplicated)

### F06 (High, Probable) — 青龙凭据可通过明文/query 路径泄露

- Evidence: `network_security_config.xml:5` + `AndroidManifest.xml:22` 全局明文放行；
  `ApiUtil.java:646-655` `client_secret` 位于 GET query；`ConfigScreen.kt:252-258` URL 无 scheme 校验；
  `ApiUtil.java:691-696` 凭 token 读取全部 env。
- Attacker prerequisites: 用户按内网习惯把青龙地址填为 `http://`，攻击者与流量同路径；或 HTTPS 下
  secret 进入服务端/代理日志。
- Impact: CLIENT_ID/SECRET → qlToken → `/open/envs` 读取全部 BJXD 账号 JWT。
- Fix: secret 改 POST body/头；URL 强制 `https:`（内网明文需显式确认 + 局部 `domain-config`）；移除
  `base-config` 明文许可与 manifest 全局开关。
- Verify: 抓包确认 secret 不在 URL；`http://` 保存被拒；HTTPS 域名禁止明文。

### F12 (Medium, Probable) — WebView 无白名单 + token 覆盖跳过服务端校验

- Evidence: `AddUserScreen.kt:186,192-203,221-223`；`AddUserActivity.kt:85-99`（`hid` 命中本地账号后直接
  `saveUser`，不调 `getUserInfo`）。
- Prerequisites: 需让 WebView 加载攻击者可控页面（登录站开放重定向/页面被攻陷）；仅系统 CA 受信，网络
  MITM 门槛高。
- Impact: 受害者账号 token 被伪造 JWT 静默覆盖（无效 token 导致持续失败，或攻击者账号被纳入本地/青龙同步）。无
  JS→Java 接口，不能执行原生代码。
- Fix: `WebViewClient.shouldOverrideUrlLoading` 仅允许 `bm2-wx.bluemembers.com.cn` 及必要跳转；所有
  token 分支先由服务端 `getUserInfo` 校验，`hid` 仅作"是否已存在"优化。
- Verify: 注入含目标 hid 的伪造 JWT，断言 `saveUser` 未调用；WebView 点击外部链接被拦截。

### F18 (Medium, Confirmed) — 凭据明文存储

- Evidence: `StorageUtil.java:19-33,93-334`；`User.java:19-25`。
- Prerequisites: 需 root/物理取证/能读取应用数据目录；普通未 root 设备受 MODE_PRIVATE +
  备份排除保护，无远程读取路径。
- Impact: 一次获取全部账号 JWT + AI/百度 Key + 青龙凭据。
- Fix: Android Keystore 支撑的 AES-GCM 或 `EncryptedSharedPreferences`；保留备份排除；提供清除密钥入口；旧明文数据一次性迁移。
- Verify: 写后读取 `app_config.xml` 不含可识别字面量；升级迁移测试。

### F19 (Medium, Confirmed) — 任务日志未排除备份（上轮修复不完整）

- Evidence: `TaskLogStore.java:21` 写 `filesDir/last_task_log.txt`；`backup_rules.xml:2-5`、
  `data_extraction_rules.xml:3-8` 仅排除
  sharedpref；日志含昵称、打码手机号、hid、题目/积分（TaskRunner.java:203-205,281,563）。
- Prerequisites: 能访问用户云备份或恢复后的设备。
- Impact: 账号标识与行为数据离开设备边界；不含原始 token。
- Fix: 两份规则同时 `<exclude domain="file" path="last_task_log.txt"/>`；或 `allowBackup="false"`
  ；日志设保留上限。
- Verify: `adb backup` 解包后无该日志文件。

### F20 (Low, Probable) — AI Key 可随用户自定义 URL 外发

- Evidence: `ConfigScreen.kt:179-186`、`ApiUtil.java:615`（Authorization 头发给任意 URL）。
- Prerequisites: 社工诱导用户填入攻击者控制的 URL。
- Fix: 保存时强制 `https:` + 域名变更确认，或白名单端点。
- Verify: `http://` 与外域 HTTPS 的拒绝/确认测试。

### F21 (Medium, Confirmed) — 发布加固缺失

- Evidence: `app/build.gradle:6-8`（弱口令回退）、`:40-45`（release 无签名、`minifyEnabled false`）、
  `proguard-rules.pro` 模板。
- Fix: 缺属性时构建失败；release 签名外部化；启用 R8 并为 Gson 模型补 keep 规则。
- Verify: `assembleRelease` 检查签名与 mapping。

### F30 (High, Confirmed / Needs validation) — git 历史凭据残留

- Evidence: `git show 7d54d9b8` 测试文件中的青龙 secret/token（已脱敏，未在报告打印）；HEAD 仅占位符。
- Prerequisites: 凭据仍有效 + 有人检出历史。
- Fix: 轮换青龙 secret/token；评估历史重写；pre-commit 密钥扫描。
- Verify: 旧凭据鉴权失败；`git log -S` 不再命中。

### F23 (Info, Confirmed) — 依赖校验缺失

- Evidence: 无 `gradle/verification-metadata.xml`；`settings.gradle:4-6,24-26` 国内镜像优先。
- Fix: 生成 verification metadata，CI strict 模式。

## Privacy

- 数据最小化：手机号打码展示；无分析/第三方采集 SDK；无原始 token 进入日志。
- 保留期：任务日志保留至下次任务（`TaskLogStore` 覆盖写），无显式 TTL（F19 关联）。
- 用户控制：可删除单个账号（MainActivity.kt:165-169）；无"清除全部密钥"入口（F18 关联）。

## Conclusion

平台边界（exported/权限/PendingIntent/CA）配置良好；主要风险集中在 **密钥链路**：明文存储、明文流量与
query secret、WebView 输入信任、备份遗漏、历史凭据。所有结论均有攻击者前提，未断言默认即被利用。无真机抓包/备份验证，状态
COMPLETE_WITH_LIMITATIONS。
