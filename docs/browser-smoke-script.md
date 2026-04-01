# Browser Smoke Script

## Purpose

这个脚本把当前已经真机验证过的浏览器命令 smoke 固化成严格串行的 PowerShell 流程，避免多条命令并行时互相污染页面状态。

脚本入口：

- `scripts/browser-smoke.ps1`

## What It Verifies

脚本当前覆盖两条关键链路：

- `daily`:
  - `read-console`
  - `open https://example.com`
  - `read-text`
- `dev` + `workspace_webview`:
  - `open https://httpbin.org/forms/post`
  - `read-text`
  - 验证页面停留在表单页并包含 `Customer name:`

默认脚本把上面这部分作为稳定 smoke。

可选严格交互链路：

- `dev` + `workspace_webview`:
  - `type`
  - `click` radio
  - `click` submit
  - `read-text`
  - `read-dom`

当使用严格交互模式时，脚本会显式校验提交后的页面正文中包含：

- `"custname": "smoke-script"`
- `"size": "medium"`

## Usage

最小运行：

```powershell
pwsh -File .\scripts\browser-smoke.ps1 -DeviceSerial 192.168.0.100:33123 -AndroidSdkRoot 'F:\Program\AS\Android\Sdk'
```

如果还要在执行前重新构建并安装 debug 包：

```powershell
pwsh -File .\scripts\browser-smoke.ps1 `
  -DeviceSerial 192.168.0.100:33123 `
  -AndroidSdkRoot 'F:\Program\AS\Android\Sdk' `
  -Build `
  -Install
```

如果还要开启严格交互链路：

```powershell
pwsh -File .\scripts\browser-smoke.ps1 `
  -DeviceSerial 192.168.0.100:33123 `
  -AndroidSdkRoot 'F:\Program\AS\Android\Sdk' `
  -StrictInteractive
```

默认安装的 debug APK 是：

- `app/termux-app-custom/app/build/outputs/apk/debug/termux-app_apt-android-7-debug_arm64-v8a.apk`

如果目标设备不是 `arm64-v8a`，可通过 `-DebugApkVariant` 指定：

- `arm64-v8a`
- `armeabi-v7a`
- `universal`
- `x86`
- `x86_64`

## Notes

- 脚本依赖设备上已可用的 `adb` 无线或有线连接。
- 命令调用基于 `adb shell` + `run-as com.termux` + `termux-browser`。
- 脚本会主动把 `com.termux/.app.TermuxActivity` 拉到前台，再开始 smoke。
- 如果某一步失败，脚本会直接抛错并停止，不会继续执行后续步骤。
- `-StrictInteractive` 适合在当前版本需要追交互稳定性时使用；它会把 `dev workspace` 的输入、点击和表单提交也纳入强断言。
