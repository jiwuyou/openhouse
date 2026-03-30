# Android OpenClaw-Style Execution Base Stage Acceptance

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-24
- Language: zh-CN
- Related:
  - `docs/prd.md`
  - `docs/architecture.md`
  - `app/termux-app-custom/docs/system-design.md`
  - `app/termux-app-custom/docs/mode-config.md`

## 2. Purpose

本文档定义“尽快正式可用”目标下的阶段验收标准。

它不追求一次性把全部理想能力做完，而是明确：

- 哪些能力必须先稳定，才能进入下一阶段
- 哪些能力可以保留为内测或实验特性
- 哪些问题属于正式发布前的阻断项

## 3. Release Principle

面向“尽快正式可用”的发布策略如下：

- 第一优先级是让 `daily` 和 `dev` 两个前台可见模式达到稳定可用
- `automation` 不应因为概念完整性而阻塞首个正式版本
- 任何未达标能力都必须明确降级为：
  - 内测
  - 实验特性
  - 默认关闭
- 不允许在正式发布说明中宣称当前实现并未真实满足的能力

## 4. Mode Release Policy

建议采用如下发布节奏：

| 模式 | Stage 1 | Stage 2 | Stage 3 | Stage 4 |
| --- | --- | --- | --- | --- |
| `daily` | 内部可用 | 小范围试用 | RC 必须稳定 | 正式发布 |
| `dev` | 内部可用 | 小范围试用 | RC 必须稳定 | 正式发布 |
| `automation` | 实验中 | 内部试用 | 可选 Beta | 满足条件后再正式发布 |

结论：

- 首个“正式可用”版本可以只对外承诺 `daily` 和 `dev`
- `automation` 只有在真正满足后台执行、会话边界和恢复能力后，才应升级为正式能力

## 5. Current Baseline

基于当前仓库状态，可确认的基线如下：

- 自定义 Termux 分支已能成功 `assembleDebug`
- 前台可见浏览器链路已接入 `TermuxActivity`
- 浏览器命令桥已支持：
  - `open`
  - `click`
  - `type`
  - `scroll`
  - `read-text`
  - `read-dom`
  - `read-console`
- mode config 已接入运行时
- `task_defaults` 与部分 `result_delivery` 已开始落地

当前仍不应直接视为“正式发布就绪”的点：

- `automation` 的 `isolated session` 还不是真隔离容器
- 背景 headless runtime 仍处于早期实现阶段
- 导出 receiver 与明文流量策略仍需发布级收口
- 尚未看到完整回归清单、兼容矩阵和发布说明

## 6. Stage Definition

### Stage 1: Internal Alpha

目标：

- 建立“同机可演示、同机可反复跑”的内部稳定基线

入口条件：

- Debug APK 可稳定构建
- 至少 1 台真实 Android 设备可安装运行

验收项：

- App 可正常启动，不出现首次启动即崩溃
- 用户可在同一界面看到终端和浏览器视图
- `termux-browser open <url>` 可打开外部页面
- `click`、`type`、`scroll` 可在前台可见浏览器工作
- `read-text`、`read-dom`、`read-console` 返回机器可读 JSON
- 浏览器登录态在应用重进后仍保留
- 浏览器下载文件可落到共享工作目录
- 文件上传可从共享工作目录选取文件
- mode config 缺失时可使用默认策略启动
- 错误场景能返回非零退出码

出阶段产物：

- 内部演示视频或操作记录
- 最小命令清单
- 已知问题列表

阻断项：

- 任一核心命令经常性卡死或无返回
- 启动崩溃或命令桥经常失联
- 登录态或共享目录完全不可用

### Stage 2: Controlled Beta

目标：

- 让少量外部用户或测试者可持续使用 `daily` / `dev`

入口条件：

- Stage 1 全部通过
- 已有最小问题跟踪清单

验收项：

- `daily` 模式单 WebView 浏览体验稳定
- `dev` 模式三面板布局稳定
- 本地开发预览链路可用：
  - Agent 修改共享目录文件
  - 启动本地服务
  - 浏览器可访问本地预览地址
- SSH 进入 Termux 后可调用浏览器命令
- `task_defaults` 可被 `--task-default` 正常引用
- `result_delivery=notification` 和可见面板状态更新可工作
- 页面加载、表单提交、链接跳转在常见页面上可稳定工作
- 至少覆盖以下设备维度：
  - 1 台 Android 10/11
  - 1 台 Android 13/14
- 关键失败场景有可读日志

出阶段产物：

- 小范围试用包
- Beta 使用说明
- 基础回归清单 v1

阻断项：

- `daily` / `dev` 任一模式存在高频崩溃
- 远程调用链路不可复现
- 本地预览场景不稳定，无法支撑开发闭环

### Stage 3: Release Candidate

目标：

- 形成可对外发布的正式候选版本

入口条件：

- Stage 2 全部通过
- `daily` / `dev` 的产品范围冻结

验收项：

- 发布范围明确写入文档：
  - `daily` 和 `dev` 为正式能力
  - `automation` 若未达标，则必须标注为实验特性
- 兼容矩阵完成：
  - 命令支持项
  - Android 差异
  - 已知不兼容项
- 安全收口完成：
  - 明文流量范围有明确限制或明确发布说明
  - 导出组件边界有明确保护或明确限制说明
- 升级与重装路径验证完成
- 发布构建可成功产出
- 核心回归清单连续通过 2 轮
- 无未关闭 P0
- 无未评估 P1

出阶段产物：

- RC 包
- 发布说明草案
- 兼容矩阵
- 回归报告

阻断项：

- 仍存在发布级安全边界不清的问题
- 仍存在高概率数据丢失或登录态异常
- 对外承诺与实际实现不一致

### Stage 4: General Availability

目标：

- 对外正式发布首个“正式可用”版本

入口条件：

- Stage 3 全部通过
- 发布包、说明、回滚方案齐备

验收项：

- 正式签名包可安装
- 发布说明中清楚区分：
  - 正式能力
  - 实验能力
  - 暂不支持能力
- `daily` / `dev` 的核心场景均已通过最终 smoke test
- 安装、首次启动、工作目录初始化、核心命令、退出重进、升级后启动均通过
- 崩溃采集、问题上报、基本运维信息可用

出阶段产物：

- 正式发布包
- 正式发布说明
- 已知问题文档
- 下一阶段路线图

## 7. Core Acceptance Cases

无论处于哪个阶段，以下用例应作为统一回归基线。

### 7.1 Terminal and Browser

- 启动 App
- 看到终端与浏览器视图
- 终端可进入 shell
- `termux-browser` 命令可调用

### 7.2 Visible Browser Actions

- `open https://example.com`
- `click --selector ...`
- `type --selector ... --text ...`
- `scroll --delta-y ...`
- `read-text`
- `read-dom`
- `read-console`

### 7.3 Shared Workspace

- 打开共享目录中的本地文件或本地站点
- 上传文件时能选到共享目录文件
- 下载文件后终端中可见

### 7.4 Session Persistence

- 登录某真实站点
- 退出 App
- 重进 App
- 登录态仍存在

### 7.5 Remote Path

- SSH 进入 Termux
- 执行浏览器命令
- 浏览器动作成功返回 JSON

## 8. Public Release Blockers

以下问题在正式发布前必须明确关闭，或明确降级处理：

- 将并不真实隔离的 `automation isolated session` 对外宣传为已完成
- 将实验中的 headless runtime 当作正式稳定能力承诺
- 未限制或未说明 `usesCleartextTraffic`
- 导出 receiver 存在可被滥用的入口而无保护说明
- 没有兼容矩阵却声称“兼容 OpenClaw”
- 没有回归清单就开始正式发布

## 9. Fastest Path Recommendation

如果目标是“尽快正式可用”，建议执行顺序如下：

1. 先把 `daily` 和 `dev` 做到 Stage 3
2. 首发版本只把 `daily` / `dev` 作为正式能力
3. `automation` 保留为：
   - 内测
   - 实验能力
   - 默认关闭或不对外承诺
4. 发布后再单独推进 `automation` 的：
   - 真正会话隔离
   - 后台执行稳定性
   - 生命周期恢复

## 10. Suggested Follow-Up Documents

基于本文档，下一步建议补齐：

- `docs/openclaw-compatibility-matrix.md`
- `docs/release-checklist.md`
- `docs/smoke-test-cases.md`
- `docs/security-release-notes.md`
- `docs/upload-automation-design.md`
