# Android OpenClaw-Style Execution Base Compatibility Matrix

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-25
- Language: zh-CN
- Related:
  - `docs/release-precheck-2026-03-24.md`
  - `docs/release-scope.md`
  - `docs/smoke-test-cases.md`

## 2. Build Baseline

| Item | Value |
| --- | --- |
| App version | `0.118.0` |
| Version code | `118` |
| Package name | `com.termux` |
| Package variant | `apt-android-7` |
| `minSdkVersion` | `21` |
| `targetSdkVersion` | `28` |
| `compileSdkVersion` | `36` |

## 3. Verified Compatibility Matrix

| Area | Value | Status | Notes |
| --- | --- | --- | --- |
| Device | `PEYM00` | Verified | Android 13 真机 |
| Android OS | `13` | Verified | 当前唯一真机验证环境 |
| ABI | `arm64-v8a` | Verified | debug / release 安装通过 |
| Build artifact | Debug arm64 APK | Verified | 已重新构建并重新安装 |
| Build artifact | Release arm64 APK | Verified | 可安装，但当前仍是临时 test key |
| Mode | `daily` | Verified | 单浏览器 UI + `open/read-text` 真机通过 |
| Mode | `dev` | Verified | 上 `Browser Preview` / 中 `TERMUX CORE` / 下 `Full Controller` |
| Mode | `automation` | Experimental | 不纳入首发正式承诺 |
| Browser network | HTTPS | Verified | `example.com` / `httpbin.org` 已验证 |
| Browser network | 外部 HTTP | Verified | 按产品决策保持放开 |
| Browser network | `127.0.0.1` / `localhost` | Verified | 真机本机 `18080` 预览已通过 |
| Shared workspace | 下载到共享目录 | Verified | 已写入 `home/downloads` |
| Shared workspace | 从共享目录上传 | Verified | `DocumentsUI -> Termux provider` 已通过 |
| Local preview | `/data/data/com.termux/files/home/...` | Verified | workspace 文件预览已通过 |
| Local preview | `/data/user/0/com.termux/files/home/...` | Not Verified | 当前未确认命中相同预览路由 |
| Session persistence | Cookie | Verified | 重启 App 后仍存在 |
| Session persistence | LocalStorage | Not Yet Executed | 本轮未单独补做显式回归 |
| Remote invoke | SSH 路径 | Not Verified | 当前 bootstrap 环境未完成真机验收 |
| Release signing | 正式发布签名 | Blocked | 当前 release 使用仓库 `testkey_untrusted.jks` |

## 4. Interpretation

当前最小兼容结论：

- 可以确认“当前版本已在 Android 13 / arm64 真机上具备 `daily/dev` 的首发基础可用性”
- 不能确认“当前版本已经完成跨设备兼容性验证”
- 不能确认“当前版本已经具备正式对外分发所需的最终签名和 SSH 路径验收”

## 5. Current Release Meaning

这份矩阵支持的口径应是：

- `daily/dev` 在当前主验证设备上已达到候选发布水平
- `automation` 仍然只是实验能力
- 正式发布前仍需补：
  - 最终签名方案
  - SSH 路径 smoke
  - 至少一轮更完整的真机回归
