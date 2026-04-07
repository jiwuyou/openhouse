# Android Execution Base Current Status

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-04-02
- Language: zh-CN
- Scope: consolidate work completed after the 2026-03-31 status snapshot
- Related:
  - [docs/current-status-2026-03-31.md](/C:/Users/24045/termuweb/docs/current-status-2026-03-31.md)
  - [app/termux-app-custom/docs/termux-browser.md](/C:/Users/24045/termuweb/app/termux-app-custom/docs/termux-browser.md)
  - [app/termux-app-custom/docs/system-design.md](/C:/Users/24045/termuweb/app/termux-app-custom/docs/system-design.md)
  - [scripts/browser-smoke.ps1](/C:/Users/24045/termuweb/scripts/browser-smoke.ps1)

## 2. Executive Summary

截至 2026-04-02，本轮新增并确认了以下结论：

- `termux-browser screenshot` 已落地到代码，并通过真机命令链验证。
- `daily/control_webview` 截图链路可用。
- `dev` 在终端前台时，对后台 `workspace_webview` 的截图链路也可用。
- 截图默认落点为 `~/downloads`，显式 `--output` 经过路径别名修复后也可用。
- 应用显示品牌已从 `Termux` 改为 `openhouse`，但包名仍保持 `com.termux`。
- `app/termux-app-custom` 已整理为可推送状态并已推送到新的远程仓库。
- 手机 `192.168.0.102:41155` 上已安装最新 debug APK，并确认桌面图标标题为 `openhouse`。
- Ubuntu 内 OpenClaw 已完成最小初始化，`gateway` 的 bind/discovery 配置问题已定位清楚。

当前最重要的新增结论不是“全部完成”，而是：

- 浏览器命令桥已经从“读写交互”扩展到了“页面截图”。
- `openhouse` 品牌化已经开始，但底层仍是 `com.termux` 兼容体系。
- OpenClaw 在 Ubuntu/proot 中的主要卡点已经从“未初始化”收敛为“gateway 后台稳定启动与网络发现兼容”。

## 3. Code Changes Completed

### 3.1 Browser Screenshot Command Added

本轮新增了 `termux-browser screenshot` 命令。

能力边界：

- 支持可见浏览器 runtime 的 viewport 截图
- 默认保存到 `~/downloads`
- 支持 `--output <path>`
- `automation/headless` 暂不支持截图

主要涉及文件：

- [BrowserCommandRequest.java](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/java/com/termux/app/browser/BrowserCommandRequest.java)
- [TermuxBrowserWorkspace.java](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/java/com/termux/app/browser/TermuxBrowserWorkspace.java)
- [TermuxBrowserController.java](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/java/com/termux/app/browser/TermuxBrowserController.java)
- [TermuxHeadlessBrowserController.java](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/java/com/termux/app/browser/TermuxHeadlessBrowserController.java)
- [termux-browser.md](/C:/Users/24045/termuweb/app/termux-app-custom/docs/termux-browser.md)

### 3.2 Screenshot Path Alias Fix

Android 私有目录存在两种常见别名：

- `/data/data/com.termux/...`
- `/data/user/0/com.termux/...`

截图路径校验最初对这两个别名处理过严，导致 `--output ~/downloads/...` 在某些调用路径下报：

- `INVALID_OUTPUT_PATH`

本轮已修复为在工作区校验前先归一化路径别名。

涉及文件：

- [TermuxBrowserController.java](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/java/com/termux/app/browser/TermuxBrowserController.java)

### 3.3 Daily Toolbar Visibility Improved

`daily` 模式下地址栏本身可点击，但视觉层级过弱，容易被误判为“被压住”或“不可用”。

本轮已做最小可见性增强：

- toolbar 背景加深对比
- toolbar 增加 padding 和阴影层次
- display/edit URL 区增加明确的圆角背景

这次调整的目标是：

- 不改变交互行为
- 先提升可见性和可辨识度

涉及文件：

- [TermuxBrowserController.java](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/java/com/termux/app/browser/TermuxBrowserController.java)

### 3.4 Branding Changed To `openhouse`

本轮已把应用显示品牌改为 `openhouse`，但刻意没有修改包名。

已变更：

- app 显示名
- 插件 placeholder 名称
- 部分顶层 UI 标题文案
- README 项目介绍

仍保持不变：

- package name: `com.termux`
- 数据目录
- 与 `com.termux` 生态相关的路径和兼容语义

涉及文件：

- [app/build.gradle](/C:/Users/24045/termuweb/app/termux-app-custom/app/build.gradle)
- [strings.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/values/strings.xml)
- [README.md](/C:/Users/24045/termuweb/app/termux-app-custom/README.md)

## 4. Repository State

### 4.1 App Repository

实际 Android 应用仓库位于：

- [app/termux-app-custom](/C:/Users/24045/termuweb/app/termux-app-custom)

本轮已完成：

- 工作树清理
- 本地调试截图和 crash/log 文件忽略规则补充
- 新远程仓库配置
- 分支改为 `main`
- 推送到：
  - `git@github.com:jiwuyou/openhouse.git`

当前重要提交包括：

- `c9377b06` Add browser runtime modes and command bridge
- `86852de5` Handle workspace path aliases for screenshots
- `f731d86c` Improve daily toolbar visibility
- `51810e76` Rename app branding to openhouse
- `adfdb6ff` Replace upstream README with openhouse overview

### 4.2 Root Workspace Repository

根仓库：

- [C:/Users/24045/termuweb](/C:/Users/24045/termuweb)

本轮新增并已提交：

- 状态文档
- smoke 脚本

根仓库目前没有配置新的远程，应用代码主体也不在根仓库中发布。

## 5. Device Verification Completed

### 5.1 Device `192.168.0.102:41155`

本轮已完成：

- 通过无线调试重新连接设备
- 安装最新 debug APK
- 确认 launcher 上应用显示名为 `openhouse`

### 5.2 Screenshot Verification

已确认以下两条链路成立：

#### `daily/control_webview`

验证过程：

- `termux-browser open https://example.com`
- `termux-browser screenshot`

结果：

- 返回 `ok=true`
- 生成 PNG 文件
- 文件真实存在于 `~/downloads`

#### `dev/workspace_webview` While Terminal Surface Is Foreground

验证过程：

- 切换到 `dev`
- 前台保持 `terminal` surface
- 对后台 `workspace_webview` 执行：
  - `open https://httpbin.org/forms/post --mode dev --target-context workspace_webview`
  - `screenshot --mode dev --target-context workspace_webview`

结果：

- 返回 `ok=true`
- 生成 PNG 文件
- 证明“终端前台截图后台浏览器页面”是成立的

## 6. OpenClaw On Ubuntu Status

### 6.1 What Has Been Confirmed

在手机 `192.168.0.102:41155` 上：

- Ubuntu rootfs 已安装
- `openclaw` / `node` / `npm` 已存在
- Termux `sshd` 已完成最小配置
- 已可通过 ADB 转发的本地 SSH 进入 Termux
- OpenClaw 已完成最小非交互初始化：
  - workspace 已创建
  - `~/.openclaw/openclaw.json` 已生成
  - gateway token 已生成

### 6.2 What Was Wrong

之前 `18789` 无法访问，至少有两个原因：

1. 配置里原本写的是：

- `gateway.bind = "loopback"`

这意味着只能监听：

- `127.0.0.1:18789`

2. Ubuntu/proot 下的 network discovery 路径会触发：

- `uv_interface_addresses returned Unknown system error 13`

这来自 OpenClaw 的 Bonjour/mDNS 依赖路径，不是单纯的端口配置问题。

### 6.3 What Has Been Changed

本轮已在 Ubuntu 配置中改成：

- `gateway.bind = "lan"`
- `discovery.mdns.mode = "off"`

并验证过前台启动日志中出现：

- `listening on ws://0.0.0.0:18789`

这说明配置方向已经正确。

### 6.4 Remaining OpenClaw Gap

虽然前台验证表明 gateway 能监听 `0.0.0.0:18789`，但后台稳定运行仍未完全收口。

当前更准确的状态是：

- OpenClaw 初始化已完成
- `18789` 可通过正确配置进入监听
- 但 Ubuntu/proot 环境下的 discovery / process lifetime 仍需进一步收口

## 7. Security Notes

本轮在 OpenClaw 配置检查中看到：

- gateway token 已写入配置文件
- Web search provider token 已写入配置文件
- `gateway.controlUi.allowInsecureAuth=true`

这些信息不应继续扩散到文档、日志、截图或公开仓库说明中。

建议后续动作：

- 旋转本轮暴露过的 token / API key
- 将长期密钥迁移到更合适的 secret 注入方式
- 重新审查 OpenClaw 本地配置中的敏感字段

## 8. Recommended Next Steps

建议后续优先级如下：

1. 为 OpenClaw 增加一个稳定的 Ubuntu 启动脚本
   - 明确设置 `OPENCLAW_DISABLE_BONJOUR=1`
   - 在 `lan` / `loopback` 场景下选择合适的 bind
2. 为 `daily` 顶栏再做一轮真机视觉回归
   - 确认地址栏可读性是否已经达到“明显可见”
3. 如果准备正式发布 `openhouse`
   - 切换正式签名
   - 明确 README 中的安装兼容说明
   - 审查是否需要进一步统一品牌文案
