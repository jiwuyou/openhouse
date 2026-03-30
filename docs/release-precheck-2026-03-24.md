# Android OpenClaw-Style Execution Base Release Precheck

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-24
- Language: zh-CN
- Scope: Local precheck only
- Related:
  - `docs/release-checklist.md`
  - `docs/release-scope.md`
  - `docs/smoke-test-cases.md`

## 2. Purpose

本文档记录 2026-03-24 在当前工作区内执行的首轮发布前预检结果。

注意：

- 本文档只覆盖当前机器上可以完成的检查
- 真机交互链路、登录态、上传下载、SSH 路径等仍需单独执行 smoke test

## 3. Overall Result

当前结论：

- 本机构建链路已基本打通，且已完成首轮真机 smoke，但仍不能直接进入正式发布

主要原因：

1. `release` APK 虽已可安装，但当前仍使用仓库 test key，不是最终正式签名方案
2. 真机 smoke 仍只是首轮最小链路，不是完整回归
3. SSH 路径仍未完成真机验收

## 4. Precheck Summary

| Item | Status | Notes |
| --- | --- | --- |
| Debug build | Pass | `:app:assembleDebug` 已成功 |
| Release build | Pass | 已改为资产读取 bootstrap，`:app:assembleRelease` 成功 |
| Unit test | Pass | 升级 Robolectric 后，`testDebugUnitTest` 通过 |
| Release APK install | Pass | 已改为使用仓库 test key 签名，arm64 release APK 已安装到真机 |
| Real-device smoke | Partial Pass | Android 13 真机已验证启动、`daily` 默认 `open/read-text`、`daily` 单浏览器 UI、`dev` 三栏 UI、`dev` workspace `open/read-text`、`task-default`、data URL 上的 `type/click/scroll/read-console`、cookie 持久化、下载到共享目录、本地 workspace 文件预览、上传到共享目录、`result_delivery=workspace_webview/control_webview`、localhost 开发预览 |
| Scope freeze | Pass | 已冻结 `daily/dev` 正式，`automation` 实验 |
| Release docs | Pass | 已生成阶段验收、范围、清单、smoke 用例，并已补最小兼容矩阵、发布说明草案、已知问题、回滚方案 |
| Security boundary | Pass | `BrowserCommandReceiver` 已改为内部组件；按产品决策保留全局 cleartext 支持 |
| Full device smoke test | Blocked | 仍未覆盖 dev/SSH/上传下载/登录态等完整链路 |

## 5. Executed Checks

### 5.1 Debug Build

Command:

```powershell
.\gradlew.bat :app:assembleDebug
```

Result:

- Pass

Artifacts observed:

- `app/build/outputs/apk/debug/termux-app_apt-android-7-debug_universal.apk`
- ABI split debug APKs also present

### 5.2 Release Build

Command:

```powershell
.\gradlew.bat :app:assembleRelease
```

Result:

- Pass

Observed result:

- `app/build/outputs/apk/release/termux-app_apt-android-7-release_universal.apk`

Interpretation:

- 原先 `app` 模块的 bootstrap native `incbin` 链路已移除
- 之前的 NDK OOM 阻断已消除

### 5.3 Release APK Installability

Command:

```powershell
adb install app/build/outputs/apk/release/termux-app_apt-android-7-release_arm64-v8a.apk
```

Result:

- Pass

Observed result:

- arm64 release APK 已成功安装到 Android 13 真机
- 当前 release APK 使用仓库内 `testkey_untrusted.jks` 签名
- release 构建已改为按 ABI 分包，避免 universal release 打包 OOM

Interpretation:

- release 候选包现在已经具备“可安装”条件
- 但它仍不是最终正式签名方案，只能视为候选发布包

### 5.4 Unit Tests

Command:

```powershell
.\gradlew.bat testDebugUnitTest
```

Result:

- Pass

Observed result:

- `testDebugUnitTest` 已完整执行通过
- 之前的失败已定位为旧版 Robolectric 与当前 JDK/SDK 组合不兼容
- 升级 Robolectric 后问题解除

Interpretation:

- 单元测试链路已完成本机验证

### 5.5 Real-Device Smoke

Device:

- Model: `PEYM00`
- Android: `13`
- Wireless debugging was verified on:
  - `192.168.0.105:44851`
  - `192.168.0.102:35245`

Validated on device:

- App 安装成功（debug APK）
- arm64 release APK 安装成功
- `TermuxActivity` 可正常启动
- `com.termux` 进程存活
- `termux-browser` CLI 已生成
- `daily` 默认命令链已验证：
  - `termux-browser open https://example.com`
  - `termux-browser read-text`
- `dev` workspace 命令链已验证：
  - `termux-browser open https://example.com --mode dev --target-context workspace_webview --session-mode shared`
  - `termux-browser read-text --mode dev --target-context workspace_webview --session-mode shared`
- `dev` 三栏 UI 已验证：
  - 前台模式切换条可见
  - `Daily / Dev / Auto` 三个模式按钮在真机上可见
  - 点击 `DEV` 后，前台布局为：
    - 上：`Browser Preview`
    - 中：`TERMUX CORE`
    - 下：`Full Controller`
  - 上下两块浏览器与中间终端为三个独立可见区域
- `daily` 前台 UI 已验证：
  - 模式切换条可见
  - 默认仅显示单一浏览器区域
  - 未出现额外 workspace/terminal 面板
- `result_delivery=workspace_webview` 真机链已验证：
  - `termux-browser open https://example.com --mode dev --target-context workspace_webview --result-delivery workspace_webview --session-mode shared`
  - UI 层级中可见 workspace 区域显示 `open succeeded: https://example.com/`
- `result_delivery=control_webview` 真机链已验证：
  - `termux-browser open https://example.com --result-delivery control_webview`
  - UI 层级中可见 control browser 区域显示 `open succeeded: https://example.com/`
- `task-default` 真机链已验证：
  - `termux-browser open https://example.com --task-default open_url`
  - 结果已按 `dev` / `workspace_webview` 路由返回
- data URL 页面真机链已验证：
  - `open`
  - `type`
  - `click`
  - `scroll`
  - `read-text`
  - `read-console`
- Cookie 持久化已验证：
  - 通过 `https://httpbin.org/cookies/set?smoke=1`
  - 重启 App 后读取 `https://httpbin.org/cookies`
  - `smoke=1` 仍存在
- 下载到共享目录已验证：
  - 通过 `httpbin` 附件响应链接
  - 文件已成功写入 `home/downloads/browser-smoke.txt`
- 本地 workspace 文件预览已验证：
  - 文件已写入 `home/workspace-preview.html`
  - 通过 `/data/data/com.termux/files/home/workspace-preview.html` 打开成功
  - `read-text` 返回 `Workspace Preview OK`
- 上传到共享目录已验证：
  - 页面触发 `input type=file`
  - 系统 `DocumentsUI` picker 成功弹出
  - 能进入 `Termux` 提供的文件来源
  - 能看到并选择 `upload-smoke.txt`
  - 页面最终回显 `upload-smoke.txt`
- 本地开发预览链路已验证：
  - 在手机本机起 `127.0.0.1:18080` 测试 HTTP 服务
  - `termux-browser open http://127.0.0.1:18080`
  - `read-text` 返回 `Local Preview OK`
- release 候选包已在真机安装成功
- release 候选包可正常拉起 `TermuxActivity`

Important finding:

- 首轮真机测试发现 `daily` 默认 `session_mode` 与 visible runtime 冲突
- 已修复为 `shared`
- 修复后，在真机上默认 `open` 和 `read-text` 已通过
- `dev` 模式若显式指定 `workspace_webview`，当前也已在真机通过
- `task-default` 最初无效，原因是 CLI 默认值总是覆盖任务默认值
- 修复后，`task-default` 在未提供真实 `mode-config.json` 的情况下已可工作
- 使用 data URL 页面可以在不依赖本地 workspace 文件写入的情况下完成浏览器交互 smoke
- 本地 workspace 文件预览已可工作，但当前命中路由的路径是 `/data/data/com.termux/files/home/...`
- 同一文件若用 `/data/user/0/com.termux/files/home/...` 打开，则当前实现不会命中 workspace 预览路由
- 通过 `httpbin` 附件响应已验证“浏览器下载 -> 共享目录”链路
- 上传链路最初未通过：
  - 触发 `input[type=file]` 后，最初不会切出系统 picker
  - 中途已把这类情况收敛成明确失败 `FILE_CHOOSER_NOT_OPENED`
  - 后续修复后，现已在真机通过
- 前台 UI 验收必须以“当前真机已安装 APK”为准：
  - 仅看工作区代码或旧安装包状态，容易误判模式条和三栏布局是否真正生效
  - 本轮已通过重新安装 debug APK + UI hierarchy dump 重新确认 `daily/dev` 布局
- release 包安装后，由于应用不是 debuggable，`run-as` 无法继续进入沙箱，因此 release 深链路自动化 smoke 不能复用 debug 包的 adb 校验方式
- `BrowserCommandReceiver` 已改为 `exported=false`
- 在该收口后，debug 包真机上的默认 `open/read-text` 仍继续通过

## 6. Static Release Risks

### 6.1 Cleartext Traffic

Observed:

- `AndroidManifest.xml` 启用了 `android:usesCleartextTraffic="true"`

Impact:

- 已按产品决策保留
- 理由是项目首发目标更接近真实浏览器体验
- 本地开发预览使用的 `127.0.0.1` / `localhost` 明文链路也因此天然保持可用

### 6.2 Exported Browser Receiver

Observed:

- `BrowserCommandReceiver` 已改为内部组件

Current mitigation:

- 已有 `request_id` 格式校验
- 已有请求目录存在性校验

### 6.3 Automation Boundary

Observed:

- `automation` 仍未达到正式能力标准
- `isolated session` 仍不是真正隔离容器

Release decision:

- 只能作为实验能力存在

## 7. Checks Not Yet Executed

以下项在本轮预检中未执行，仍必须在发布前完成：

- SSH 调用路径 smoke

补充说明：

- 当前真机 bootstrap 环境下未发现 `ssh`、`sshd`、`pkg`、`apt` 可执行文件
- 这不应直接视为“终端能力未达标”
- 正确验收口径应是：
  - 是否保持原版 Termux 的能力边界
  - 是否仍允许按原版 Termux 的方式后续获得这些能力
  - 是否在发布说明中避免错误承诺“开箱即预装 ssh/pkg/apt”

## 8. Release Gate Status

### Blockers

- [x] Release build 成功
- [x] Release APK 可安装
- [ ] 完整真机 smoke test 执行完成

### Required Before Formal Release

- [x] 单元测试完成
- [x] 兼容矩阵至少形成最小版本
- [x] 已知问题列表生成
- [x] 发布说明生成
- [x] 回滚方案生成

## 9. Recommended Next Actions

建议按以下顺序继续：

1. 在真机上继续执行剩余 `smoke-test-cases.md`
2. 补 SSH 路径 smoke
3. 切换到最终正式签名方案
4. 最后再决定：
   - 是否当前版本直接正式发布
   - 或者先发 `daily/dev` 的受控 Beta

## 10. Current Recommendation

当前推荐动作不是“立即正式发布”，而是：

- 进入真机验收与发布边界收口阶段

原因：

- 发布文档已经形成最小闭环
- debug / release 产物已经存在
- 本机测试链路已经打通
- 首轮真机最小链路已经跑通
- release 候选包已具备安装条件
- 剩余工作已主要集中在完整真机验收、SSH 链路与最终签名
