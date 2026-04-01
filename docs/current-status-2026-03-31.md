# Android OpenClaw-Style Execution Base Current Status

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-31
- Language: zh-CN
- Scope: consolidate repository docs plus latest code and real-device verification completed on 2026-03-31
- Related:
- [docs/prd.md](/C:/Users/24045/termuweb/docs/prd.md)
- [docs/architecture.md](/C:/Users/24045/termuweb/docs/architecture.md)
- [docs/release-scope.md](/C:/Users/24045/termuweb/docs/release-scope.md)
- [docs/known-issues.md](/C:/Users/24045/termuweb/docs/known-issues.md)
- [docs/browser-smoke-script.md](/C:/Users/24045/termuweb/docs/browser-smoke-script.md)

## 2. Executive Summary

截至 2026-03-31，项目已经具备以下可明确确认的状态：

- Android 侧 `Termux + visible browser + command bridge` 基础方案成立。
- `daily` 前台命令链已达到真机可用水平。
- `dev` 的 `workspace_webview` 基础读链路已达到真机可用水平。
- 真机上已经通过命令成功验证：
  - `open`
  - `read-text`
  - `read-dom`
  - `read-console`
  - `type`
  - `click`
- `dev/workspace_webview` 之前存在的“跨模式/跨面板串状态”问题已在代码中修复，并已重新编译、安装、回归验证。
- 一份可重复执行的 PowerShell 真机 smoke 脚本已经落库，可用于后续回归。

当前最重要的结论不是“已经可以正式发布”，而是：

- 项目已经从“方案验证”进入“可持续回归和收口发布边界”的阶段。
- `daily` 可视浏览器命令控制链已形成稳定主线。
- `dev` 主链路可用，但交互链路仍存在偶发不稳定，不宜直接宣称完全稳定。
- 正式发布仍受正式签名、SSH 路径、部分持久化与完整回归覆盖度限制。

## 3. Product Scope Snapshot

项目定位没有变化：

- Android 单 App 宿主
- 同一 App 内提供终端和浏览器
- 命令侧尽量兼容 OpenClaw 风格
- 终端与浏览器共享工作目录
- 浏览器保留真实登录态与 Cookie/LocalStorage

当前推荐发布边界仍然是：

- `daily`: 正式能力
- `dev`: 正式能力
- `automation`: 实验能力

## 4. Verified Working Areas

### 4.1 Build and Install

当前已确认：

- `:app:compileDebugJavaWithJavac` 通过
- `:app:assembleDebug` 通过
- debug APK 可重新安装到 arm64 真机

2026-03-31 本轮已实际完成：

- 修改代码后重新编译
- 重新产出 debug APK
- 重新安装 `arm64-v8a` debug APK 到真机

### 4.2 Daily Browser Command Chain

本轮在真机 `192.168.0.100:33123` 上已实际验证：

- `termux-browser read-console`
- `termux-browser open https://example.com`
- `termux-browser read-text`
- `termux-browser read-dom`
- `termux-browser type`
- `termux-browser click`

其中一个明确闭环是：

- 打开 `https://httpbin.org/forms/post`
- 输入 `custname`
- 选择 `size=medium`
- 点击提交
- 页面返回 `https://httpbin.org/post`
- 返回正文中包含提交后的表单值

### 4.3 Dev Workspace Read Chain

在本轮修复后，以下链路已通过真机串行回归：

- `open https://httpbin.org/forms/post --mode dev --target-context workspace_webview --session-mode shared`
- `read-text --mode dev --target-context workspace_webview --session-mode shared`
- `read-dom --mode dev --target-context workspace_webview --session-mode shared`

修复后的实际结果表明：

- `open`、`read-text`、`read-dom` 现在能稳定指向同一页面
- 不再出现本轮修复前常见的“open 成功但 read 读到旧页/别的页”问题

### 4.4 Result Delivery

本轮重新验证后，以下路径可确认：

- `result_delivery=control_webview` 的 `open` 本轮已重新成功，不再复现先前单次测试里的 `TIMEOUT`
- `dev` 默认的 `workspace_webview,terminal` 路由仍可工作

## 5. Fixes Completed On 2026-03-31

### 5.1 Fixed Cross-Mode Visible Browser State Collision

本轮定位并修复的问题：

- 浏览器控制器在桥接层只按 `target_context` 注册和查找
- 浏览器 tabs/history/bookmarks 的持久化状态也只按 `target_context` 分桶

这会导致：

- `daily/control_webview` 和 `dev/workspace_webview` 之间发生状态串扰
- `dev/workspace_webview open` 后，后续 `read-text` 或 `read-dom` 偶尔读到旧页面
- 某些情况下出现 `open` 超时或页面读写不一致

本轮修复内容：

- 控制器注册与查找改为按 `mode + target_context` 分键
- 浏览器持久化状态键改为按 `mode + target_context` 分桶
- `TermuxActivity` 在创建控制器时显式传入当前 mode

涉及文件：

- [TermuxBrowserBridge.java](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/java/com/termux/app/browser/TermuxBrowserBridge.java)
- [TermuxBrowserController.java](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/java/com/termux/app/browser/TermuxBrowserController.java)
- [TermuxActivity.java](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/java/com/termux/app/TermuxActivity.java)

修复后已实际验证：

- `dev/workspace_webview` 串行 `open -> read-text -> read-dom` 已对齐到同一目标页

## 6. Smoke Script Status

### 6.1 New Script Added

本轮新增：

- [browser-smoke.ps1](/C:/Users/24045/termuweb/scripts/browser-smoke.ps1)
- [browser-smoke-script.md](/C:/Users/24045/termuweb/docs/browser-smoke-script.md)

### 6.2 What The Script Covers

默认 smoke：

- 检查设备在线
- 拉起 `TermuxActivity`
- `daily`:
  - `read-console`
  - `open https://example.com`
  - `read-text`
- `dev/workspace_webview`:
  - `open https://httpbin.org/forms/post`
  - `read-text`
  - 断言页面仍停留在表单页并包含 `Customer name:`

可选严格模式 `-StrictInteractive`：

- 在 `dev/workspace_webview` 上继续执行：
  - `type`
  - `click` radio
  - `click` submit
  - `read-text`
  - `read-dom`

### 6.3 Why Strict Interactive Is Optional

本轮的一个重要经验是：

- `dev/workspace_webview` 的基础打开与读取链路在修复后已经稳定
- 但连续交互动作在当前版本下仍有偶发不稳定
- 如果把交互闭环直接作为默认 smoke 的硬门槛，会让脚本本身不稳定，失去“固定回归基线”的价值

因此当前策略是：

- 默认 smoke 只覆盖稳定主链路
- 更严格的交互闭环保留为专项稳定性验证开关

### 6.4 Script Execution Result

本轮已实际执行默认脚本：

- 命令：
  - `pwsh -File .\scripts\browser-smoke.ps1 -DeviceSerial 192.168.0.100:33123 -AndroidSdkRoot 'F:\Program\AS\Android\Sdk'`
- 结果：通过

本轮脚本输出摘要：

```json
{
  "device": "192.168.0.100:33123",
  "daily_url": "https://example.com/",
  "dev_url": "https://httpbin.org/forms/post",
  "strict_interactive": false
}
```

## 7. Remaining Risks and Gaps

### 7.1 Still Not Release-Ready

当前仍不能直接下结论“可正式对外发布”，主要原因：

- release 仍未切正式签名
- SSH 调用路径仍未完成真机 smoke
- `automation` 仍是实验能力
- LocalStorage 持久化尚未补做独立显式回归

### 7.2 Dev Interactive Stability Still Needs Work

本轮虽然已经确认：

- `dev/workspace_webview` 的读链路问题已修复

但仍观察到：

- 在某些连续操作下，`type` 或 `button submit` 仍可能偶发不稳定
- 因此不宜把 `dev` 的完整交互自动化链路直接记成“完全稳定”

当前更准确口径应是：

- `dev/workspace_webview` 基础命令链可用
- 交互闭环具备实现并在人工串行验证中曾经通过
- 但自动化稳定性仍需继续加强

### 7.3 Workspace Preview Alias Still Not Closed

仍待确认：

- `/data/data/com.termux/files/home/...`
- `/data/user/0/com.termux/files/home/...`

是否能完全命中同一套预览语义。

### 7.4 Cross-Device Coverage Is Still Thin

当前已验证的主设备仍然集中在：

- Android 13
- arm64 真机

因此仍不能把当前结果外推成“已完成跨设备兼容验证”。

## 8. Recommended Next Steps

建议按以下顺序推进：

1. 继续收敛 `dev/workspace_webview` 的严格交互链稳定性，使 `-StrictInteractive` 可稳定通过。
2. 补做 SSH 进入 Termux 后调用浏览器命令的真机 smoke。
3. 补做 LocalStorage 持久化的显式真机回归。
4. 确认 workspace preview 的路径别名语义。
5. 在至少另一台 Android 版本不同的设备上跑默认 smoke。
6. 准备正式签名并更新发布口径。

## 9. Current Practical Release Reading

如果只用一句话描述当前状态：

- 项目已经拥有可工作的 Android 浏览器命令执行主链路；`daily` 已接近发布候选，`dev` 基础链路可用但交互稳定性仍需继续打磨；今天已经修复了一个真实的跨模式状态串扰问题，并把默认真机 smoke 固化成了脚本。
