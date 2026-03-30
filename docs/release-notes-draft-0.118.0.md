# Android OpenClaw-Style Execution Base Release Notes Draft 0.118.0

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-25
- Language: zh-CN
- Audience: 内部候选发布说明

## 2. Release Positioning

本版本的目标是形成“尽快正式可用”的首个候选版本。

本次对外正式能力范围：

- `daily`
- `dev`

本次不作为正式能力承诺：

- `automation`

## 3. Major Changes

### 3.1 Frontend Runtime

- `TermuxActivity` 已接入 App 内浏览器运行时
- 增加前台模式切换条：
  - `Daily`
  - `Dev`
  - `Auto`
- `daily` 现在为单浏览器前台布局
- `dev` 现在为三段前台布局：
  - 上 `Browser Preview`
  - 中 `TERMUX CORE`
  - 下 `Full Controller`

### 3.2 Browser Command Capability

- 已支持核心浏览器动作：
  - `open`
  - `click`
  - `type`
  - `scroll`
  - `read-text`
  - `read-dom`
  - `read-console`
- `task-default` 路由已可工作
- `result_delivery=workspace_webview`
- `result_delivery=control_webview`
- `result_delivery=notification`

### 3.3 Shared Workspace

- 浏览器下载可写入共享目录
- 浏览器上传可通过系统 picker 进入 `Termux` provider
- 本地 workspace 文件预览已可工作
- 本地 `127.0.0.1` 开发预览已在真机验证

### 3.4 Session And Runtime Persistence

- `daily` 默认 session 已修正为 `shared`
- Cookie 持久化已在真机验证
- `daily/dev` 前台浏览器链路已具备稳定基础可用性

### 3.5 Build And Packaging

- bootstrap 已改为从 APK assets 读取
- `:app:assembleDebug` 通过
- `:app:assembleRelease` 通过
- `testDebugUnitTest` 通过
- release 已改为按 ABI 分包
- arm64 release APK 已能安装到真机

## 4. Security Boundary

- `BrowserCommandReceiver` 已改为内部组件
- `usesCleartextTraffic=true` 按产品决策保留
- 本版本允许真实浏览器式外部 HTTP 访问与本地 loopback HTTP 预览

## 5. Known Limitations For This Draft

- `automation` 仍为实验能力
- SSH 路径仍未完成真机 smoke
- 当前 release 包仍使用仓库 test key，不是最终正式签名方案
- `/data/user/0/com.termux/files/home/...` 路径别名尚未确认完全等价于当前 workspace 预览路由

## 6. Recommended Public Message

如果当前版本作为受控发布或内部 Beta，对外表述应为：

- Android 上的终端 + 可编程浏览器宿主
- `daily/dev` 已可用
- `automation` 仍为实验能力

不应对外表述为：

- 已完成稳定后台自动化
- 已完成完整 OpenClaw 兼容
- 已具备真正隔离的多浏览器 profile
