# Android OpenClaw-Style Execution Base Rollback Plan

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-25
- Language: zh-CN

## 2. Rollback Trigger

任一情况出现，应执行回滚评估：

- 真机出现启动失败
- `daily` 前台主浏览器不可用
- `dev` 三栏布局不可用
- 浏览器核心命令大面积失败
- 上传下载链路回归损坏
- 候选包安装后出现严重数据目录初始化问题

## 3. Rollback Target

当前建议的回滚目标不是“回退到所有旧代码”，而是回退到上一个已知可安装、可启动、可执行 `daily` 基础命令链的 APK。

## 4. Rollback Assets To Preserve

回滚前应保留：

- 当前候选 APK
- 上一个已知稳定 APK
- 当前 `mode-config.example.json`
- 当前发布说明和已知问题文档
- 本轮真机验证记录

## 5. Rollback Steps

### 5.1 Package Rollback

- 停止继续分发当前候选 APK
- 重新安装上一个已知稳定 APK
- 确认应用包名仍为 `com.termux`

### 5.2 Runtime Verification After Rollback

至少确认以下项目：

- App 可启动
- `daily` 可见
- `daily` `open/read-text` 可用
- `dev` 若仍在范围内，则三栏可见
- 下载和上传基础链路未失效

### 5.3 Documentation Rollback

- 更新发布说明
- 标记本轮回退原因
- 在已知问题中记录回退触发条件

## 6. Current Practical Advice

由于当前版本已经改动到前台布局和浏览器运行时，回滚判断必须以“真机已安装 APK 的实际行为”为准，不应只凭工作区代码状态做结论。
