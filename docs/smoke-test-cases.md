# Android OpenClaw-Style Execution Base Smoke Test Cases

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-24
- Language: zh-CN
- Related:
  - `docs/release-checklist.md`
  - `docs/release-scope.md`

## 2. Execution Rule

每次候选发布包至少跑完一次本清单。

记录要求：

- 设备型号
- Android 版本
- 包版本
- 执行人
- 结果：
  - Pass
  - Fail
  - Blocked

## 3. Device Matrix

最低建议覆盖：

- Device A: Android 10/11
- Device B: Android 13/14

## 4. Smoke Cases

### Case 01: Install and Launch

步骤：

1. 安装 APK
2. 启动 App
3. 等待主界面完成初始化

期望：

- App 不崩溃
- 能看到终端视图
- 能看到浏览器视图

### Case 02: Terminal Availability

步骤：

1. 在终端执行 `pwd`
2. 在终端执行 `echo ok`

期望：

- 命令可执行
- 输出可见

### Case 03: Browser CLI Exists

步骤：

1. 在终端执行 `which termux-browser`
2. 在终端执行 `termux-browser read-console`

期望：

- `termux-browser` 存在于 PATH
- 返回 JSON

### Case 04: Open External Page

步骤：

1. 执行 `termux-browser open https://example.com`

期望：

- 返回 JSON
- `ok=true`
- 浏览器打开目标页面

### Case 05: Read Text

步骤：

1. 在成功打开页面后执行 `termux-browser read-text`

期望：

- 返回 JSON
- 能读到页面文本

### Case 06: Read DOM

步骤：

1. 执行 `termux-browser read-dom`

期望：

- 返回 JSON
- 能读到页面 DOM

### Case 07: Scroll

步骤：

1. 执行 `termux-browser scroll --delta-y 600`

期望：

- 返回 JSON
- 页面有可见滚动或返回滚动位移

### Case 08: Type and Click

步骤：

1. 打开一个含输入框和按钮的测试页面
2. 执行 `type`
3. 执行 `click`

期望：

- 输入成功
- 点击成功
- 若为表单提交，页面状态有变化

### Case 09: Console Readback

步骤：

1. 打开一个会触发控制台错误的测试页
2. 执行 `termux-browser read-console`

期望：

- 返回 JSON
- 能看到 console error 记录

### Case 10: Daily Mode

步骤：

1. 执行 `termux-browser open https://example.com --mode daily`

期望：

- 页面在 `daily` 模式下成功打开
- 布局符合单主浏览器模式

### Case 11: Dev Mode

步骤：

1. 执行 `termux-browser open https://example.com --mode dev`

期望：

- 页面在 `dev` 模式下成功打开
- control webview / terminal / workspace webview 布局可见

### Case 12: Task Default

步骤：

1. 执行 `termux-browser open https://example.com --task-default open_url`

期望：

- 返回 JSON
- 请求按配置中的 task default 解析

### Case 13: Notification Delivery

步骤：

1. 执行 `termux-browser open https://example.com --result-delivery notification`

期望：

- 返回 JSON
- 系统通知栏出现结果摘要通知

### Case 14: Visible Panel Delivery

步骤：

1. 执行 `termux-browser open https://example.com --mode dev --result-delivery workspace_webview`

期望：

- 返回 JSON
- 对应浏览器面板状态文本更新

### Case 15: Shared Workspace Download

步骤：

1. 打开一个可下载文件的页面
2. 触发下载
3. 在终端检查共享目录下载路径

期望：

- 文件已落到共享目录
- 终端中可见

### Case 16: Shared Workspace Upload

步骤：

1. 在共享目录准备测试文件
2. 打开上传页面
3. 触发文件选择
4. 从共享目录选择文件

期望：

- 能看到共享目录
- 文件可被选中

### Case 17: Session Persistence

步骤：

1. 登录一个测试站点
2. 退出 App
3. 重启 App
4. 回到该站点

期望：

- 登录态仍保留

### Case 18: SSH Path

步骤：

1. 通过 SSH 进入 Termux
2. 执行 `termux-browser open https://example.com`

期望：

- 命令可执行
- 返回 JSON
- App 内浏览器完成动作

## 5. Failure Recording Template

建议按以下格式记录失败：

- Case ID:
- Device:
- Android Version:
- Build Version:
- Command:
- Actual Result:
- Expected Result:
- Logs / Screenshot:
- Severity:

## 6. Exit Rule

首发正式版最小通过要求：

- Case 01 - 11 全部通过
- Case 13 - 18 中，凡属于本次正式承诺范围的项必须通过
- 任一 P0 失败即停止发布
