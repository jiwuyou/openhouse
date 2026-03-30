# Android OpenClaw-Style Execution Base Release Scope

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-24
- Language: zh-CN
- Related:
  - `docs/stage-acceptance.md`
  - `docs/prd.md`
  - `app/termux-app-custom/docs/system-design.md`

## 2. Purpose

本文档用于对首个“尽快正式可用”版本做范围冻结。

目标不是描述所有长期规划，而是明确：

- 这次发布对外承诺什么
- 这次发布明确不承诺什么
- 哪些能力只能作为实验特性存在

## 3. Release Decision

首个正式可用版本采用如下范围：

- `daily`：正式能力
- `dev`：正式能力
- `automation`：实验能力，不计入正式版本承诺

## 4. Officially Supported in This Release

### 4.1 Runtime Form

- Android 单 App 宿主
- 同一 App 内同时提供：
  - 终端视图
  - 浏览器视图
- 浏览器与终端共享工作目录

### 4.2 Terminal Side

- 基于 Termux 终端能力
- 支持本机命令行调用
- 支持 SSH 进入 Termux 后调用浏览器命令

### 4.3 Browser Command Set

本次正式支持以下命令动作：

- `open`
- `click`
- `type`
- `scroll`
- `read-text`
- `read-dom`
- `read-console`

### 4.4 Visible Modes

#### `daily`

正式承诺：

- 面向普通浏览使用
- 单主浏览器视图
- 支持在当前用户上下文中触发浏览器命令

#### `dev`

正式承诺：

- 面向开发预览和调试
- 三面板工作模式
- 支持：
  - control webview
  - terminal panel
  - workspace webview

### 4.5 Workspace

正式承诺：

- 浏览器与终端共享同一工作目录
- 浏览器下载文件可写入共享目录
- 浏览器上传文件可访问共享目录

### 4.6 Session

正式承诺：

- 前台可见浏览器保留登录态
- Cookie 持久化
- LocalStorage 持久化

## 5. Explicitly Not Officially Supported Yet

以下能力本次发布不作为正式承诺：

- 多标签页
- 截图
- 录屏
- 自定义远程协议
- 完整 OpenClaw 全量兼容
- 真正独立的后台浏览器会话容器
- 真正隔离的 `automation isolated session`

## 6. Experimental Features

### `automation`

当前状态：

- 允许存在
- 可继续开发
- 可供内部验证
- 不应在正式发布说明中作为稳定能力承诺

当前原因：

- headless runtime 还未完成正式稳定性验证
- 会话隔离尚未达到发布级语义
- 生命周期恢复与后台持续执行能力尚未完全收口

发布策略：

- 可以默认隐藏
- 可以标记为 Beta / Experimental
- 可以保留配置入口
- 不应作为首发核心卖点

## 7. Required Public Messaging

正式发布时，对外表述必须符合以下约束：

- 可以说：
  - 提供 Android 上的终端 + 可编程浏览器宿主
  - 支持 `daily` 和 `dev` 的正式前台工作流
  - 支持命令行驱动浏览器执行核心动作
- 不可以说：
  - 已完全支持 OpenClaw
  - 已完成稳定后台自动化
  - 已支持真正隔离的多会话浏览器 profile

## 8. Release Exit Rule

只有在以下条件同时满足时，当前版本才能视为范围收口：

- 发布说明与本文档一致
- Stage 3 / Stage 4 所要求的 `daily` / `dev` 验收项通过
- `automation` 未被错误包装为正式能力
- 已知未完成项已明确降级为实验特性或未支持项
