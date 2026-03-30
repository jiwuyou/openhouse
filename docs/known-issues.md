# Android OpenClaw-Style Execution Base Known Issues

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-25
- Language: zh-CN

## 2. P0 / Release Blockers

### 2.1 Release Signing Is Temporary

Current state:

- 当前 release APK 使用仓库内 `testkey_untrusted.jks`

Impact:

- 可作为候选发布包安装验证
- 不应直接作为最终正式对外分发包

Required action:

- 切换到正式签名方案

### 2.2 SSH Path Not Yet Verified

Current state:

- 当前真机环境尚未完成“SSH 进入 Termux 后调用浏览器命令”的完整 smoke

Impact:

- 无法把 SSH 路径计入正式已验收能力

Required action:

- 补做真机 SSH smoke

## 3. P1 / Important Known Issues

### 3.1 Workspace Preview Path Alias Is Not Fully Closed

Current state:

- `/data/data/com.termux/files/home/...` 已验证可进入 workspace 预览
- `/data/user/0/com.termux/files/home/...` 尚未确认命中相同路由

Impact:

- 对本地文件预览的路径语义不能做“两个别名完全等价”的承诺

### 3.2 Automation Is Still Experimental

Current state:

- `automation` 模式仍未达到正式能力标准

Impact:

- 不应写入首发正式承诺
- 可以保留实现和入口，但必须标注实验性

### 3.3 LocalStorage Persistence Still Needs Explicit Regression

Current state:

- Cookie 持久化已验证
- LocalStorage 持久化尚未单独补做显式真机回归

Impact:

- 发布口径应优先使用“前台浏览器登录态和 cookie 已验证”
- 不要在未补测前把 LocalStorage 单独写成强承诺

### 3.4 Tabs Workflow Is Only Partially Verified

Current state:

- 简版 tabs 入口和 tabs overlay 已接入当前浏览器壳
- 最新 debug 包已完成真机 smoke 的最小验证：
  - tabs 入口可见
  - overlay 可弹出
  - `New Tab` 计数可增加
- 但以下路径仍未单独补测：
  - tab 切换恢复
  - tab 关闭
  - 多 tab 下状态保持

Impact:

- 不能把完整 tabs 工作流直接记为“已正式验收”
- 当前只能算“最小 tabs 能力已通过，完整 tabs 能力待补测”

### 3.5 Mozilla Toolbar Line Is Pinned To 140.0 For Compatibility

Current state:

- 当前并未采用 `reference-browser` 对应的 nightly 版组件
- `browser-toolbar` 固定在 `140.0`
- 原因是更高版本已把 `minSdk` 提升到 `26`

Impact:

- 当前阶段优先保证兼容现有 `minSdkVersion=21`
- 若后续决定抬高最低 Android 版本，才应考虑切到更高组件版本

### 3.6 Local Build Depends On Vendored Maven Artifacts

Current state:

- 工作区增加了 `vendor/maven`
- 用于承接当前机器上 Gradle/TLS 无法稳定拉取的部分 Kotlin 工件

Impact:

- 当前构建链已恢复
- 但后续若切换机器或 CI，需要显式同步这套本地仓库策略

### 3.7 Tabs Persistence Is Implemented But Not Yet Device-Verified

Current state:

- tabs 持久化逻辑已经接入：
  - Activity 重建恢复
  - `SharedPreferences` 简化恢复
- 当前仍未形成稳定的真机自动化证据证明“冷启动后 tabs 数量和当前 tab 能正确恢复”

Impact:

- 不能把 tabs 持久化直接记为已验收能力
- 当前口径应为“实现已落地，真机待复验”

### 3.8 Browser Menu Workflow Is Implemented But Not Yet Fully Re-Smoked

Current state:

- overflow menu 相关代码已经接入并通过本机构建
- 但设备侧最终验证本轮被锁屏/SystemUI 干扰打断

Impact:

- 不能把 `History / Bookmarks / Add Bookmark / New Tab` 菜单流程直接记为已验收
- 当前只能算“实现已进入 APK，真机待补测”

### 3.9 History And Bookmarks Are Implemented But Not Yet Fully Device-Verified

Current state:

- history / bookmarks 数据结构、持久化和 overlay 已在代码中接入
- 当前 APK 已完成构建
- 已在真实站点 `https://www.sohu.com/` 上验证：
  - history 面板打开
  - bookmarks 面板打开
  - `Add Bookmark` 后 bookmarks 列表出现搜狐 URL
- 但以下路径仍未补测：
  - remove bookmark
  - history item reopen after app restart

Impact:

- 不能把完整 history/bookmarks 工作流全部记为正式已验收能力
- 当前可记为“history/bookmarks 基础闭环已通过，完整工作流待补测”

## 4. Product Decision Notes

### 4.1 External HTTP Is Intentionally Left Open

Current state:

- `usesCleartextTraffic=true` 保留

Meaning:

- 这不是缺陷单
- 这是按产品方向做出的明确决策
- 目的是保持更接近真实浏览器的访问能力，并确保本地 loopback HTTP 预览可用
