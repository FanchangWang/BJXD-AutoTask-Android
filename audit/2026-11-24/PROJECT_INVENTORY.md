# Project Inventory

- Audit run ID: AUDIT-2026-11-24-01
- Date: 2026-11-24
- Repository / branch: `C:\Users\guyue\code\BJXD-AutoTask-Android` / `main`
- Commit: `c51b87cc5356d76cc4e6bb2f4f3c9df7cca3f15b`
- Mode: full
- Auditor: OpenCode agent (skill: `android-compose-code-audit`)
- Build baseline: ./gradlew detekt lint test assembleDebug --no-daemon → BUILD SUCCESSFUL (60 tasks)

## Environment and versions

| Item                                 | Detected value                                                               | Evidence                                   |
|--------------------------------------|------------------------------------------------------------------------------|--------------------------------------------|
| Gradle wrapper                       | 9.9.1                                                                        | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin                | 9.5.1                                                                        | `gradle/libs.versions.toml` (`agp`)        |
| Kotlin                               | 2.2.10 (AGP built-in Kotlin)                                                 | `gradle/libs.versions.toml` (`kotlin`)     |
| Compose BOM / compiler plugin        | 2026.09.00 / `org.jetbrains.kotlin.plugin.compose` 2.2.10                    | `gradle/libs.versions.toml`                |
| compileSdk / targetSdk / minSdk      | 37 / 37 / 25                                                                 | `app/build.gradle`                         |
| Java toolchain                       | source/target 17; Gradle daemon JDK 21                                       | `app/build.gradle`, local env              |
| DI / navigation / database / network | none / Activity+Intent / SharedPreferences+file / OkHttp 5.3.2 + Gson 2.13.2 | build files + source                       |

## Modules

| Module | Type                | Source sets                                                                         | Depends on                                            | Notes                     |
|--------|---------------------|-------------------------------------------------------------------------------------|-------------------------------------------------------|---------------------------|
| `app`  | Android application | `src/main/java` (Java), `src/main/kotlin` (Kotlin), `src/test/java`, `src/main/res` | AndroidX, Material3, Compose, OkHttp, Gson, lifecycle | single module, no flavors |

## File coverage

| Path                                                                               | Module/source set | Role                      | Applicable domains          | Status   | Finding IDs |
|------------------------------------------------------------------------------------|-------------------|---------------------------|-----------------------------|----------|-------------|
| `app/src/main/kotlin/com/guyuexuan/bjxd/MainActivity.kt`                           | app/main          | UI/state holder           | D02,D03,D04,D09,D10,D12     | reviewed |             |
| `app/src/main/kotlin/com/guyuexuan/bjxd/AddUserActivity.kt`                        | app/main          | UI/WebView boundary       | D02,D03,D04,D08,D10         | reviewed |             |
| `app/src/main/kotlin/com/guyuexuan/bjxd/ConfigActivity.kt`                         | app/main          | UI/persistence boundary   | D02,D03,D06,D08             | reviewed |             |
| `app/src/main/kotlin/com/guyuexuan/bjxd/TaskActivity.kt`                           | app/main          | UI/service binding        | D02,D03,D04,D12             | reviewed |             |
| `app/src/main/kotlin/com/guyuexuan/bjxd/TaskLogActivity.kt`                        | app/main          | UI/log viewer             | D02,D03,D09                 | reviewed |             |
| `app/src/main/kotlin/com/guyuexuan/bjxd/SyncActivity.kt`                           | app/main          | UI/coroutine owner        | D02,D03,D04,D09,D12         | reviewed |             |
| `app/src/main/kotlin/com/guyuexuan/bjxd/AnswerActivity.kt`                         | app/main          | UI/notification result    | D02,D03,D08                 | reviewed |             |
| `app/src/main/kotlin/com/guyuexuan/bjxd/sync/QinglongSync.kt`                      | app/main          | domain/data orchestration | D01,D04,D06,D07,D08,D09,D12 | reviewed |             |
| `app/src/main/kotlin/com/guyuexuan/bjxd/ui/*.kt`                                   | app/main          | UI composables            | D02,D10                     | reviewed |             |
| `app/src/main/kotlin/com/guyuexuan/bjxd/ui/common/LogView.kt`                      | app/main          | UI composable             | D02,D09,D10                 | reviewed |             |
| `app/src/main/kotlin/com/guyuexuan/bjxd/ui/theme/*.kt`                             | app/main          | design tokens             | D02,D10                     | reviewed |             |
| `app/src/main/java/com/guyuexuan/bjxd/util/ApiUtil.java`                           | app/main          | network data source       | D01,D07,D08,D12             | reviewed |             |
| `app/src/main/java/com/guyuexuan/bjxd/util/StorageUtil.java`                       | app/main          | persistence               | D01,D06,D08                 | reviewed |             |
| `app/src/main/java/com/guyuexuan/bjxd/util/AppUtils.java`                          | app/main          | platform utility          | D01,D03,D08                 | reviewed |             |
| `app/src/main/java/com/guyuexuan/bjxd/util/TaskLogStore.java`                      | app/main          | in-memory + file store    | D01,D04,D06,D09             | reviewed |             |
| `app/src/main/java/com/guyuexuan/bjxd/service/TaskService.java`                    | app/main          | foreground service        | D01,D03,D04,D08,D12         | reviewed |             |
| `app/src/main/java/com/guyuexuan/bjxd/service/TaskActionReceiver.java`             | app/main          | broadcast receiver        | D03,D08                     | reviewed |             |
| `app/src/main/java/com/guyuexuan/bjxd/task/TaskRunner.java`                        | app/main          | background task logic     | D01,D04,D07,D09             | reviewed |             |
| `app/src/main/java/com/guyuexuan/bjxd/model/*.java`                                | app/main          | model                     | D01,D06,D08                 | reviewed |             |
| `app/src/main/AndroidManifest.xml`                                                 | app/main          | platform config           | D03,D08,D11                 | reviewed |             |
| `app/src/main/res/xml/*.xml`                                                       | app/main          | security/backup config    | D06,D08,D11                 | reviewed |             |
| `app/src/main/res/values*/themes.xml`, `colors.xml`, `strings.xml`                 | app/main          | resources                 | D10,D11                     | reviewed |             |
| `app/src/test/java/.../ApiUtilTest.java`                                           | app/test          | test                      | D11                         | reviewed |             |
| `app/build.gradle`, `build.gradle`, `settings.gradle`, `gradle/libs.versions.toml` | root/app          | build config              | D05,D11,D12                 | reviewed |             |
| `.gitignore`, `.gitattributes`, `gradle.properties`, `config/detekt/detekt.yml`    | root              | repo/tooling config       | D11,D12                     | reviewed |             |
| `DESIGN_RULES.md`, `CLAUDE.md`, `Readme.md`                                        | root              | docs/agent rules          | D05,D12                     | reviewed |             |

## Tooling and safe commands

| Tool/task           | Available? | Command                               | Result                              |
|---------------------|------------|---------------------------------------|-------------------------------------|
| detektDesign        | yes        | `./gradlew detekt`                    | success                             |
| lint                | yes        | `./gradlew lint`                      | success                             |
| unit test           | yes        | `./gradlew test`                      | success (integration tests @Ignore) |
| assembleDebug       | yes        | `./gradlew assembleDebug`             | success                             |
| configuration cache | enabled    | `org.gradle.configuration-cache=true` | entry stored, no problems           |

## Exclusions and reasons

| Path/pattern                                            | Reason                              | Risk |
|---------------------------------------------------------|-------------------------------------|------|
| `build/`, `app/build/`, `.gradle/`, `.idea/`            | generated/build output              | low  |
| `app/release/`, `*.apk`, `BJXD.jks`, `local.properties` | build artifacts/secrets, gitignored | low  |
| `gradle/wrapper/gradle-wrapper.jar`                     | vendor binary                       | low  |
| mipmap/webp assets                                      | binary image resources              | low  |

## Known limitations

- No device/emulator-based runtime verification in this audit run; findings requiring runtime
  evidence are marked Needs validation.
- Backend behavior (Beijing Hyundai API, Baidu search, Zhipu GLM, Qinglong server) is outside
  repository evidence; assumptions are explicit in findings.
- No minified release build or profiling run; performance findings are static only.
