# Android OpenClaw-Style Execution Base Current Progress And Lessons

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-25
- Language: zh-CN
- Related:
- [release-precheck-2026-03-24.md](/C:/Users/24045/termuweb/docs/release-precheck-2026-03-24.md)
- [release-checklist.md](/C:/Users/24045/termuweb/docs/release-checklist.md)
- [smoke-test-cases.md](/C:/Users/24045/termuweb/docs/smoke-test-cases.md)
- [upload-automation-design.md](/C:/Users/24045/termuweb/docs/upload-automation-design.md)

## 2. Summary

截至 2026-03-25，项目已经从“文档和代码雏形”推进到“候选发布包可构建、可安装、核心命令链可在真机上跑通”的阶段。

当前最重要的结论：

- `daily` 和 `dev` 的前台浏览器命令链已经具备真实可用性
- `daily` 单浏览器 UI 和 `dev` 三栏 UI 都已经在真机重新安装后确认可见
- 前台浏览器壳已经开始从“手写控件”迁移到 Mozilla Android Components 路线
- `release` 候选包已经可以构建并安装到 arm64 真机
- 发布阻断已经从“构建链断裂”缩小到“完整真机回归 + 最终发布边界决策”

## 3. Current Progress

### 3.1 Build And Packaging

已完成：

- `:app:assembleDebug` 通过
- `:app:assembleRelease` 通过
- `testDebugUnitTest` 通过
- release 构建已支持签名并产出可安装候选包
- release 构建已调整为按 ABI 分包

当前状态：

- debug APK 可安装
- release arm64 APK 可安装
- release universal APK 不再是首要目标

### 3.2 Browser Runtime

已完成：

- App 内可见浏览器已接入 `TermuxActivity`
- `daily` 默认命令链已打通
- `dev` 的 `workspace_webview` 命令链已打通
- `Full Controller` / `daily` 主浏览器已切入 Mozilla `BrowserToolbar`
- 简版 tabs 入口和 tabs overlay 已在真机验证：
  - tabs 计数入口可见
  - 点击后 overlay 可弹出
  - 点击 `New Tab` 后计数可从 `1` 增加到 `2`
- Mozilla toolbar overflow menu 已接入本地代码：
  - `New Tab`
  - `Tabs`
  - `History`
  - `Add/Remove Bookmark`
  - `Bookmarks`
- 前台模式切换 UI 已可见
- `daily` 前台单浏览器布局已在真机确认
- `dev` 三栏布局已在真机确认：
  - 上 `Browser Preview`
  - 中 `TERMUX CORE`
  - 下 `Full Controller`
- 简版 tabs 入口与 tabs overlay 已在本地代码中落地，并已通过本机构建
- `headless` 基本控制器已存在
- `read-console` 在真机 data URL 页面上已验证可返回错误日志

### 3.3 Command Bridge

已完成：

- `termux-browser` CLI 已可自动生成
- `termux-host` CLI 已可自动生成
- request/response 目录桥已可工作
- `BrowserCommandReceiver` 已改为内部组件
- receiver 已带请求 ID 和请求目录校验

### 3.4 Config And Routing

已完成：

- mode config 运行时加载
- `daily/dev/automation` 三模式基础路由
- `task-default` 路由已打通
- `result_delivery=notification` 已在真机上验证
- `result_delivery=control_webview/workspace_webview` 状态更新路径已存在

### 3.5 Real Device Validation

已完成的真机验证：

- App 启动
- 前台模式切换条可见：
  - `Daily`
  - `Dev`
  - `Auto`
- `daily` 前台布局：
  - 单一浏览器区域
- `daily`:
  - `open`
  - `read-text`
- `dev` 前台布局：
  - 上 `Browser Preview`
  - 中 `TERMUX CORE`
  - 下 `Full Controller`
- `dev workspace`:
  - `open`
  - `read-text`
- `task-default open_url`
- data URL 页面:
  - `open`
  - `type`
  - `click`
  - `scroll`
  - `read-text`
  - `read-console`
- Cookie 持久化
- 下载到共享目录
- 本地 workspace 文件预览
- 上传到共享目录
- `result_delivery=workspace_webview`
- `result_delivery=control_webview`
- localhost 开发预览
- release arm64 APK 安装
- Mozilla toolbar/tabs 真机验证：
  - `Daily` 单浏览器已切换到 Mozilla `BrowserToolbar`
  - `Dev` 底部 `Full Controller` 已切换到 Mozilla `BrowserToolbar`
  - tabs overlay 弹出通过
  - `New Tab` 计数更新通过
- 真实站点 smoke 已开始替代最小页面 smoke：
  - `https://www.sohu.com/` 已在真机前台通过地址栏打开
  - toolbar 标题/URL 已切到搜狐实际页面
  - 在真实站点页面上，tabs overlay 仍能打开
  - `History` library overlay 已出现搜狐访问记录
  - `Bookmarks` library overlay 已打开并显示空态
  - 在搜狐页点击 `Add Bookmark` 后，`Bookmarks` 列表已出现搜狐 URL
- Mozilla overflow menu:
  - 本机构建已通过
  - 真机最终复验本轮被手机切到锁屏/SystemUI 打断
- History / Bookmarks:
  - overlay 已接入代码
  - `browser-menu` 已接入构建链
  - 当前 APK 已包含相关入口

## 4. Remaining Work

### 4.1 Still Not Fully Verified

- SSH 调用路径
- 更完整的 `dev` 工作流
- 完整 release 包真机场景回归

### 4.2 Still Not Fully Closed

- 最小兼容矩阵
- 正式发布说明
- 已知问题列表
- automation 上传直传仍处于设计阶段
- Mozilla browser shell 后续还需补更完整 tabs 工作流复验：
  - tab 切换
  - tab 关闭
  - tab 恢复后的状态保持

### 4.3 Release Boundary Decision Already Made

- `usesCleartextTraffic=true` 已按产品方向保留
- 原因：
  - 更接近真实浏览器
  - 不人为限制外部 HTTP 站点
  - 本地开发预览的 loopback 明文链路天然可用

### 4.4 Additional Constraint Found

- 当前真机 bootstrap 环境下未发现：
  - `ssh`
  - `sshd`
  - `pkg`
  - `apt`

这意味着：

- 不能把“当前 APK 未预置这些二进制”直接等同于“未对齐原版 Termux”
- 若产品口径是“终端能力按原版 Termux”，则更合理的判断是：
  - 是否保持原版 Termux 的能力边界
  - 是否仍支持后续按原版 Termux 的方式获得这些能力
  - 是否没有把“开箱即预装 ssh/pkg/apt”错误写进发布承诺

因此：

- “SSH 路径未验证”仍然是一个待补验收项
- 但“APK 当前未预装 ssh/pkg/apt”本身不应被当作首发阻断

## 5. Important Fixes Already Made

### 5.1 Daily Default Session Fix

问题：

- `daily` 默认 `session_mode=isolated`
- 但 visible runtime 不支持 isolated
- 导致无参数默认命令直接失败

处理：

- 将 `daily` 默认 session 改为 `shared`

结果：

- 真机默认 `open` / `read-text` 已通过

### 5.2 Task Default Override Fix

问题：

- CLI 会把 mode/session/target/result 的默认值直接写入请求目录
- `task_default` 即使存在，也没有机会覆盖这些字段

处理：

- 修改请求生成逻辑，让 `task_default` 只在用户未显式传参时生效

结果：

- `open_url` 任务默认值已在真机跑通

### 5.3 App Bootstrap Packaging Fix

问题：

- 原 `app` 模块通过 native `incbin` 把 bootstrap zip 打进 so
- release 构建在 NDK 汇编阶段出现 OOM

处理：

- 改为从 APK 资产读取 bootstrap zip

结果：

- 消除了 `app` 模块 bootstrap native OOM 阻断

### 5.4 Release Packaging Fix

问题：

- release 打包阶段在 `packageRelease` 上出现 OOM

处理：

- release 改为按 ABI 分包
- 重点产出 arm64 真机可安装包

结果：

- arm64 release APK 可构建、可安装

### 5.5 Unit Test Compatibility Fix

问题：

- `Robolectric 4.10` 在当前 JDK 23 / compileSdk 36 组合下出错

处理：

- 升级到 `Robolectric 4.16`

结果：

- `testDebugUnitTest` 已通过

### 5.6 Receiver Boundary Fix

问题：

- `BrowserCommandReceiver` 最初为导出组件

处理：

- 改为内部组件
- 保留 request ID / request dir 校验

结果：

- 安全边界收紧
- 真机默认命令链未受影响

### 5.7 Mozilla Browser Toolbar Integration

问题：

- 原先前台浏览器更多只是“可被命令驱动的 WebView”
- 作为正式可用浏览器，地址栏和导航壳层能力明显不足

处理：

- 引入 Mozilla Android Components 的 `browser-toolbar`
- 为兼容当前 `minSdkVersion=21`，未使用 `reference-browser` 的 nightly 依赖线，而是固定到正式仓库仍支持 `minSdk 21` 的 `140.0`
- `daily` 和 `dev` 的控制浏览器都改为复用同一套 Mozilla toolbar

结果：

- 浏览器顶部不再是手写地址栏控件
- 已具备更正式的 toolbar 结构：
  - back
  - forward
  - reload
  - title/url 区

### 5.8 Local Maven Vendor Workaround

问题：

- 当前机器上的 Gradle / TLS 环境会在解析部分 Kotlin 运行时依赖时失败
- 直接访问 Maven 仓库时，`curl` 也会被 Windows 证书吊销检查拦住

处理：

- 使用本地 `vendor/maven` 目录承接缺失工件
- 在项目根 `build.gradle` 中把本地 Maven 仓库放到最前
- 使用 `curl --ssl-no-revoke` 下载缺失工件，绕过本机证书吊销检查问题

结果：

- Mozilla toolbar 接入后的 `:app:assembleDebug` 已重新恢复通过
- 当前工作区已经具备可重复构建条件

### 5.9 Tabs Overlay Validation

问题：

- 只有 toolbar，不足以支撑“正式可用”的浏览器前台工作流
- 需要至少提供最小 tabs 入口和 tabs 管理面板

处理：

- 在控制浏览器面板中增加 tabs 计数入口
- 增加简版 tabs overlay
- 支持：
  - 打开 tabs 面板
  - 新建 tab
  - 关闭 tabs 面板

结果：

- 最新 debug 包已在真机验证：
  - tabs 入口可见
  - overlay 可打开
  - `New Tab` 后计数从 `1` 增加到 `2`

### 5.10 Tabs Persistence Implementation

问题：

- tabs 只在当前前台会话内可见，不足以支撑更正式的浏览器使用习惯

处理：

- 在控制器中增加 tabs 状态的进程内恢复
- 同时增加基于 `SharedPreferences` 的简化持久化：
  - tab 列表
  - 当前 tab 索引
  - 下一个 tab id

结果：

- 实现已落地并已通过本机构建
- 但本轮自动化真机验证未能稳定命中“重启前先创建第二个 tab”的步骤
- 因此当前只能记为“实现完成，真机待复验”，不能记为“已通过”

### 5.11 Browser Overflow Menu Integration

问题：

- toolbar 右侧空间有限
- 继续堆 action 不利于正式浏览器体验

处理：

- 接入 Mozilla `browser-menu`
- 将以下入口转入 overflow menu：
  - `New Tab`
  - `Tabs`
  - `History`
  - `Add/Remove Bookmark`
  - `Bookmarks`

结果：

- 当前代码与构建链已通过
- 设备侧最终菜单验证还需补一轮前台 smoke

### 5.12 History And Bookmarks Overlay

问题：

- 只有 tabs 还不足以支撑更像正式浏览器的前台体验

处理：

- 在控制器中增加：
  - history 记录
  - bookmarks 记录
  - library overlay
- 同时将 `browser-menu` 接入当前工程，为后续 toolbar overflow 做准备

结果：

- 当前代码和 APK 构建已通过
- `History` overlay 已在真实站点页面上通过真机验证
- `Bookmarks` overlay 已在真实站点页面上通过真机验证
- `Add Bookmark -> Bookmarks 列表出现搜狐 URL` 已通过真机验证

## 6. Practical Lessons

### 6.1 Do Not Trust “Can Build Debug” As A Release Signal

经验：

- debug 能过，不代表 release 能过
- 真实阻断很多发生在：
  - native packaging
  - resource optimization
  - final APK packaging
  - signing

结论：

- 发布前必须单独验证 release 构建链

### 6.2 Candidate Release Package Does Not Need To Be Universal First

经验：

- 当前机器资源有限
- 强行追求 universal release 包会放大打包内存压力

结论：

- 首先保证目标设备 ABI 的候选包可安装
- 当前对 arm64 真机，优先产出 `arm64-v8a` release 即可

### 6.3 Debug And Release Have Different Verification Capabilities

经验：

- debug 包可以用 `run-as`
- release 包默认不可用 `run-as`

结论：

- 深链路自动化验证应优先在 debug 包完成
- release 包验证重点放在：
  - 可安装
  - 可启动
  - 外部可见行为

### 6.4 Data URL Is A Very Useful Smoke Tool

经验：

- 本地 workspace 文件链路在某些设备/路径上不稳定
- data URL 可以快速构造稳定测试页面

它适合验证：

- `open`
- `type`
- `click`
- `scroll`
- `read-text`
- `read-console`

结论：

- data URL 非常适合做浏览器桥最小验收

### 6.5 Remote Shell Argument Details Matter

经验：

- 在远端 shell 场景下，CSS selector 若含 `#`，容易和 shell 注释语义互相干扰

结论：

- 做真机 smoke 时，优先选择简单 selector
- 或明确做好引号转义

### 6.6 Wireless ADB Endpoints Are Volatile

经验：

- 同一台手机的无线调试端口可能变化
- 配对成功不代表后续连接端口永远不变

结论：

- 真机测试过程中，应随时准备重新连 ADB
- 记录最新可用 endpoint 很重要

### 6.7 If Generated Script Logic Changes, Force Regeneration

经验：

- 仅靠内容对比不一定能及时反映设备侧脚本状态
- 设备上的 `termux-browser` 可能继续保留旧生成内容

结论：

- CLI 生成逻辑发生关键变化时，提升脚本版本号是更稳的做法

### 6.8 Path Aliases Matter For Workspace Preview

经验：

- 在真机上，同一个文件可能同时可见于：
  - `/data/data/com.termux/files/home/...`
  - `/data/user/0/com.termux/files/home/...`
- 当前 workspace 预览路由只对前者成功命中

结论：

- 真机验收时不要默认这两个路径等价
- 如果要对用户或 Agent 暴露“本地文件预览”，最好统一为明确支持的路径语义

### 6.9 File Input Validation Needs UI-Level Confirmation

经验：

- `click` 命令返回成功，不等于系统文件选择器真的已经弹出
- 对 `<input type=file>` 场景，必须同时观察：
  - 当前焦点窗口
  - UI hierarchy dump

当前观察：

- 触发 file input 后，界面仍停留在 WebView
- 当前实现已把这类情况显式归类为失败，而不是误报成功
- 失败语义为 `FILE_CHOOSER_NOT_OPENED`

结论：

- 上传链路不能只靠命令返回值判定通过
- 这项必须做 UI 级别验收

### 6.10 File Picker Flow Needed Multiple Fixes

经验：

- `<input type=file>` 不是普通按钮点击
- 即使 `click` 命令返回成功，也可能只是停留在 WebView
- 只有当：
  - 系统 `DocumentsUI` picker 真正切出
  - 能进入 `Termux` provider
  - 页面最终回显所选文件名
  才能判定上传链路通过

结论：

- 上传链路必须按“页面 -> picker -> provider -> 回页”完整闭环验收

### 6.11 Localhost Preview Is Separately Worth Validating

经验：

- data URL、文件预览、本地 HTTP 预览是三条不同能力链
- 即使前两条能过，也不能直接推断 `127.0.0.1:<port>` 就一定可用

结论：

- 本地开发预览应单独做真机验收
- 最小验证方式是：
  - 在设备本机起一个 HTTP 服务
  - 用 `termux-browser open http://127.0.0.1:<port>`
  - 再用 `read-text` 验证页面内容

### 6.12 UI Acceptance Must Follow The Installed APK

经验：

- 工作区代码已经改了，不等于手机上当前跑的就是这版
- 对前台布局这类问题，只看代码非常容易误判

结论：

- UI 验收必须至少同时满足：
  - 真机重新安装当前构建产物
  - 前台手动观察或 UI hierarchy dump 与目标布局一致

### 6.13 Mozilla Reference Browser Should Be Used As Structure, Not Nightly Dependency Copy

经验：

- `reference-browser` 很适合作为结构母本
- 但它默认依赖的是更激进的 nightly 组件版本，不适合直接原样抄到当前工程

结论：

- 应以 `reference-browser` 的结构为参考
- 具体依赖版本应单独选择与当前产品兼容的正式版本线

### 6.14 Build Infrastructure Can Block Product Work

经验：

- 真正开始接入 Mozilla 组件后，最大的第一批阻断不在 UI 代码本身，而在：
  - 依赖版本兼容
  - `minSdk` 边界
  - 本机 Gradle/TLS 环境

结论：

- 浏览器产品化阶段，构建基础设施本身就是主线工作的一部分

### 6.15 Wireless ADB Can Interrupt Verification Even When Mainline Code Is Correct

经验：

- 真机验证过程中，代码和 APK 都可能已经没问题
- 但无线 ADB 端口随时会失效，直接打断安装和 smoke

结论：

- 真机验收记录里应同步保留最新可用 endpoint
- “安装失败”与“APK 回归失败”必须明确区分

## 7. Suggested Next Steps

建议按以下顺序继续：

1. 补完整真机 smoke
2. 决定 `usesCleartextTraffic=true` 的最终发布策略
3. 形成最小兼容矩阵
4. 形成正式发布说明
5. 形成已知问题列表

## 8. Current Release Recommendation

当前建议不是“立刻正式发布”，而是：

- 进入最后一轮完整真机验收与发布文档收口

原因：

- 候选包链路已经基本完整
- 核心浏览器命令链已经可用
- 剩余问题主要集中在完整覆盖和发布口径，不再是基础实现是否成立
