# Android OpenClaw-Style Execution Base Release Checklist

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-24
- Language: zh-CN
- Related:
  - `docs/release-scope.md`
  - `docs/stage-acceptance.md`

## 2. Usage

发布前必须逐项勾选。

结论判定规则：

- 任一 `Blocker` 未完成：不可发布
- 任一 `Required` 未完成：不可正式发布
- `Optional` 未完成：可发布，但必须记录

## 3. Scope Freeze

- [ ] `Blocker` 已确认本次正式发布范围仅包含 `daily` 和 `dev`
- [ ] `Blocker` 已确认 `automation` 仅作为实验能力或不对外承诺
- [ ] `Required` 发布说明没有宣称未实现能力

## 4. Build and Package

- [ ] `Blocker` Debug 构建成功
- [ ] `Blocker` Release 构建成功
- [ ] `Required` 安装包命名、版本号、签名信息已确认
- [ ] `Required` 发布 APK 已在真实设备安装验证

## 5. Startup and Basic Flow

- [ ] `Blocker` 首次启动成功
- [ ] `Blocker` 二次启动成功
- [ ] `Blocker` 终端视图可见
- [ ] `Blocker` 浏览器视图可见
- [ ] `Required` 工作目录初始化成功

## 6. Core Browser Command Flow

- [ ] `Blocker` `open` 可正常返回
- [ ] `Blocker` `click` 可正常返回
- [ ] `Blocker` `type` 可正常返回
- [ ] `Blocker` `scroll` 可正常返回
- [ ] `Blocker` `read-text` 可正常返回
- [ ] `Blocker` `read-dom` 可正常返回
- [ ] `Blocker` `read-console` 可正常返回
- [ ] `Required` 所有核心命令失败时返回机器可读 JSON
- [ ] `Required` 所有核心命令失败时返回非零退出码

## 7. Daily Mode

- [ ] `Blocker` `daily` 模式可启动
- [ ] `Required` `daily` 模式浏览器布局正常
- [ ] `Required` `daily` 模式下浏览体验无明显 UI 错乱
- [ ] `Required` `daily` 结果通知或面板状态更新可工作

## 8. Dev Mode

- [ ] `Blocker` `dev` 模式可启动
- [ ] `Blocker` 三面板布局可见
- [ ] `Required` control webview 可响应命令
- [ ] `Required` workspace webview 可响应命令
- [ ] `Required` 终端面板与浏览器并存时布局可用

## 9. Shared Workspace

- [ ] `Blocker` 浏览器和终端看到的是同一工作目录
- [ ] `Blocker` 下载文件进入共享目录
- [ ] `Blocker` 上传文件可从共享目录选择
- [ ] `Required` 本地文件预览或本地服务预览至少有一条稳定链路可跑通

## 10. Session Persistence

- [ ] `Blocker` 前台浏览器登录态可保持
- [ ] `Required` 重启 App 后 Cookie 仍存在
- [ ] `Required` 重启 App 后 LocalStorage 仍存在

## 11. Remote and SSH Path

- [ ] `Required` SSH 进入 Termux 后可调用浏览器命令
- [ ] `Required` 远程调用与本地调用返回语义一致

## 12. Config and Routing

- [ ] `Required` 没有 `mode-config.json` 时可使用默认策略
- [ ] `Required` `--mode` 能影响运行时行为
- [ ] `Required` `--task-default` 能正常生效
- [ ] `Required` `result_delivery=notification` 可工作
- [ ] `Required` `result_delivery=control_webview|workspace_webview` 可工作

## 13. Logging and Failure Handling

- [ ] `Required` 命令失败可通过日志定位
- [ ] `Required` 页面加载失败可见错误信息
- [ ] `Required` 浏览器桥失联时可返回明确错误
- [ ] `Optional` 有最小问题收集或导出路径

## 14. Security and Release Boundary

- [ ] `Blocker` 已评估 `usesCleartextTraffic` 是否符合本次发布策略
- [ ] `Blocker` 已评估导出 receiver 的边界与风险
- [ ] `Required` 对未完全关闭的边界已有发布说明
- [ ] `Required` 不把实验能力伪装成正式能力

## 15. Regression

- [ ] `Blocker` 已执行 smoke test
- [ ] `Required` 已执行至少一轮完整回归
- [ ] `Required` 无未关闭 P0
- [ ] `Required` 无未评估 P1

## 16. Release Output

- [ ] `Blocker` 已生成正式发布说明
- [ ] `Required` 已生成已知问题列表
- [ ] `Required` 已生成回滚方案
- [ ] `Optional` 已生成兼容矩阵
