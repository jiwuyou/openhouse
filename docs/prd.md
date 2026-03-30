# Android OpenClaw-Style Execution Base PRD

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-22
- Language: zh-CN
- Source: Consolidated from requirement clarification sessions

## 2. Product Summary

本产品是一个面向 Android 的独立 App。它不是 AI 产品本身，而是为上层 AI/Agent 提供执行基座：一侧提供与 Termux 等价的终端能力，另一侧提供类似桌面 OpenClaw 的浏览器控制能力，并尽量兼容 OpenClaw 的命令格式。

产品的直接安装者是普通用户，但产品的直接调用者可以是本机 AI 应用或通过 SSH 接入的远程 Agent。用户不需要理解命令体系；命令接口主要是为 AI/Agent 提供稳定执行面。

## 3. Problem Statement

目标用户中有一类人没有电脑，但仍希望在手机上完成网站或应用相关的构建、调试、预览与外部网页操作。现有 Android 环境通常只能提供单一能力：

- 终端环境可执行命令，但缺少可编程浏览器控制。
- 浏览器可预览页面，但无法像桌面自动化工具那样被命令行系统化驱动。
- AI/Agent 若要在手机上完成完整工作流，通常需要自行拼接终端、浏览器、文件系统和会话状态。

本产品要解决的是：在 Android 上提供一个统一宿主，使 AI/Agent 能像控制电脑一样，同时控制终端和浏览器，并共享同一个工作目录与状态环境。

## 4. Target Users

### 4.1 Direct Installer

普通 Android 用户，可能没有电脑。

### 4.2 Direct Integrator / Runtime Caller

- 本机上的 AI 应用
- 通过 SSH 进入设备的远程 Agent

## 5. Product Positioning

产品定位为：

- 一个 Android 独立 App
- 一个给 AI/Agent 使用的执行基座
- 一个统一的“终端 + 浏览器 + 共享工作目录”宿主

产品不负责：

- 集成或设计 AI 模型
- 解释用户自然语言
- 设计 Agent 安全策略

## 6. Core Goals

第一阶段目标：

- 在 Android 上提供可直接复用的 Termux 级终端能力
- 在 App 内提供一个可被命令行驱动的浏览器视图
- 浏览器命令格式尽量兼容 OpenClaw
- 终端与浏览器共享工作目录
- 保留用户真实浏览会话状态，支持登录态延续

## 7. Non-Goals

第一阶段不包含：

- 自研 AI 模型或 Agent 编排层
- 多标签页浏览器
- 截图能力
- 录屏能力
- 自定义远程协议设计
- 独立云端同步系统

说明：
远程调用默认通过 SSH 进入终端环境完成，不额外为远程 Agent 设计新的网络接口作为第一阶段必需项。

## 8. Primary Scenarios

### 8.1 AI 驱动本地开发预览

1. Agent 通过命令行在共享目录创建或修改项目文件。
2. Agent 启动本地服务。
3. Agent 驱动内置浏览器打开本地地址。
4. Agent 读取页面文本、DOM 或控制台报错。
5. Agent 根据结果继续修改项目。

### 8.2 AI 操作真实外部网站

1. 用户在内置浏览器完成登录。
2. 浏览器保留登录态、Cookie、LocalStorage。
3. Agent 通过命令行驱动浏览器打开指定页面。
4. Agent 执行点击、输入、滚动和页面读取。
5. Agent 在共享目录与网页之间处理上传和下载文件。

### 8.3 远程 Agent 接入

1. 用户安装 App。
2. Agent 通过 SSH 接入终端环境。
3. Agent 在终端中调用浏览器控制命令。
4. 浏览器在 App 内执行对应动作并返回结果。

## 9. Functional Requirements

### 9.1 Shell / Terminal

- 必须提供与 Termux 能力边界一致的终端执行能力。
- 必须尽量直接复用 Termux 代码。
- 必须保持 Termux 包名不改变。
- 必须支持 SSH 接入场景。
- 必须支持在共享工作目录中执行命令。

### 9.2 Browser Runtime

第一阶段浏览器必须支持以下动作：

- 打开页面
- 点击元素
- 输入文本
- 滚动页面
- 读取页面文本
- 读取 DOM
- 获取控制台报错

第一阶段浏览器限制：

- 不支持多标签页
- 不要求截图
- 不要求录屏

### 9.3 Session Persistence

- 必须保留用户登录态
- 必须保留 Cookie
- 必须保留 LocalStorage

### 9.4 Shared Workspace

- 浏览器与终端必须共享同一工作目录
- 网页上传文件时，应能访问共享工作目录中的文件
- 网页下载文件时，应能落到共享工作目录

### 9.5 Command-Line Interface

- 必须提供命令行入口
- 命令格式应尽量兼容 OpenClaw
- 命令结果应可被 AI/Agent 程序化读取

说明：
“尽量兼容”表示第一阶段应优先对齐 OpenClaw 的核心命令结构、参数风格和返回语义；如果 Android 环境存在客观差异，应在文档中显式列出兼容差异。

### 9.6 UI Requirements

App 界面第一阶段必须至少包含：

- 浏览器视图
- 终端视图

目标是让用户能看到浏览器实际状态，也能看到终端执行环境。

## 10. User Experience Principles

- 普通用户可以完成安装并直接持有一个可被 AI 使用的宿主环境
- 浏览器状态应尽量接近真实用户浏览环境，而不是纯无头自动化环境
- 终端与浏览器应围绕“同一任务上下文”工作，而不是两个孤立子系统
- 命令调用应保持稳定、可预期、可脚本化

## 11. External Compatibility

### 11.1 Termux Compatibility

- 复用 Termux 代码
- 能力边界按 Termux
- 包名不改变

### 11.2 OpenClaw Compatibility

- 第一阶段以命令格式兼容为目标
- 优先兼容常用核心浏览器动作
- 对无法完全兼容的部分单独记录差异

## 12. Constraints

- 平台限定为 Android
- 第一阶段不处理多标签页复杂度
- 第一阶段不引入独立远程控制协议
- 第一阶段不负责上层 AI 集成
- 浏览器与终端必须在同一宿主 App 中协同工作

## 13. Key Risks

- Termux 代码复用与内置浏览器集成之间可能存在 Android 生命周期和进程模型冲突
- OpenClaw 命令兼容在 Android WebView 或内置浏览器实现上可能存在行为差异
- 共享工作目录与网页文件上传/下载之间的权限桥接容易成为实现难点
- 保留真实登录态意味着浏览器容器必须尽量稳定，避免状态丢失

## 14. MVP Scope

MVP 必须包含：

- Android App 壳
- Termux 级终端集成
- 浏览器视图
- 单标签页浏览器控制
- OpenClaw 风格命令行接口
- 打开/点击/输入/滚动/读取页面/读取 DOM/读取控制台报错
- 共享工作目录
- 上传/下载文件与共享目录打通
- 会话状态持久化

MVP 不包含：

- 多标签页
- 截图
- 录屏
- 自研 AI
- 自定义远程 Agent 协议

## 15. Milestone Plan

### Milestone 0: Architecture Validation

目标：
验证 Termux 代码复用、浏览器嵌入、共享目录和命令桥是否能在单个 Android App 中成立。

交付物：

- 最小技术验证文档
- 关键依赖与集成点列表
- OpenClaw 兼容命令最小子集定义

### Milestone 1: Runtime Foundation

目标：
建立可运行的宿主骨架。

交付物：

- App 基础框架
- 终端视图
- 浏览器视图
- 共享工作目录初始化
- 终端与浏览器进程/组件通信骨架

### Milestone 2: Browser Command Bridge

目标：
让命令行可以稳定驱动浏览器核心动作。

交付物：

- 打开页面命令
- 点击元素命令
- 输入文本命令
- 滚动页面命令
- 页面文本与 DOM 读取命令
- 控制台报错读取命令
- 命令返回格式定义

### Milestone 3: Workspace and Session Integration

目标：
打通真实使用环境。

交付物：

- 登录态持久化
- Cookie / LocalStorage 持久化
- 上传文件读取共享目录
- 下载文件写入共享目录
- 本地开发预览工作流验证

### Milestone 4: Compatibility and Stabilization

目标：
对齐 OpenClaw 使用体验并补齐稳定性。

交付物：

- OpenClaw 兼容矩阵
- 差异命令说明
- 关键失败场景回归清单
- MVP 发布候选版本

## 16. Suggested Work Breakdown

1. 先确认技术路线是否真的能在“不改 Termux 包名”的前提下成立。
2. 明确 OpenClaw 命令兼容范围，冻结 MVP 命令集合。
3. 建立终端与浏览器的桥接层。
4. 再做共享目录、上传下载与会话状态持久化。
5. 最后做兼容性、稳定性和文档收口。

## 17. Open Questions

当前仍未完全关闭的问题：

- 浏览器内核具体选型是否直接决定后续兼容成本
- OpenClaw 哪些命令属于第一阶段必兼容集合
- SSH 接入依赖 Termux 现有能力到什么程度，是否需要额外包装
- App 内浏览器与终端视图的交互方式是否需要更多可视反馈

## 18. Acceptance Criteria for MVP

满足以下条件可视为 MVP 达成：

- Agent 可通过命令行启动并使用终端环境
- Agent 可通过 OpenClaw 风格命令打开 App 内浏览器页面
- Agent 可在浏览器执行点击、输入、滚动
- Agent 可读取页面文本、DOM 和控制台报错
- 浏览器保留登录态
- 浏览器与终端共享工作目录
- 上传和下载文件都已与共享目录打通
- 用户可在同一个 App 内看到浏览器和终端视图

## 19. Next Document Candidates

在本 PRD 之后，建议继续补充：

- 命令接口规范文档
- OpenClaw 兼容矩阵
- Android 架构设计文档
- 共享目录与文件桥接详细设计
