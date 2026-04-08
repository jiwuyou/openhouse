param(
    [Parameter(Mandatory = $true)]
    [string]$DeviceSerial,

    [string]$AndroidSdkRoot = $env:ANDROID_SDK_ROOT,

    [string]$AppProjectDir = $(Join-Path $PSScriptRoot '..\app\termux-app-custom'),

    [ValidateSet('arm64-v8a', 'armeabi-v7a', 'universal', 'x86', 'x86_64')]
    [string]$DebugApkVariant = 'arm64-v8a',

    [switch]$StrictInteractive,

    [switch]$Build,

    [switch]$Install,

    [string]$AppPackage = 'com.openhouse.app',

    [string]$AppActivityClass = 'com.termux.app.TermuxActivity'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:AppFilesPath = "/data/user/0/$AppPackage/files"
$script:BrowserCliPath = "$script:AppFilesPath/usr/bin/termux-browser"
$script:AppActivityComponent = "$AppPackage/$AppActivityClass"

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Fail {
    param([string]$Message)
    throw $Message
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        Fail $Message
    }
}

function Assert-Contains {
    param(
        [string]$Value,
        [string]$Expected,
        [string]$Message
    )

    if ($null -eq $Value -or -not $Value.Contains($Expected)) {
        Fail "$Message`nExpected substring: $Expected`nActual: $Value"
    }
}

function Assert-AnyContains {
    param(
        [string]$Value,
        [string[]]$ExpectedValues,
        [string]$Message
    )

    foreach ($expected in $ExpectedValues) {
        if ($null -ne $Value -and $Value.Contains($expected)) {
            return
        }
    }

    $renderedExpected = ($ExpectedValues -join ', ')
    Fail "$Message`nExpected one of: $renderedExpected`nActual: $Value"
}

function Get-AdbPath {
    param([string]$SdkRoot)

    if ([string]::IsNullOrWhiteSpace($SdkRoot)) {
        Fail 'ANDROID_SDK_ROOT is not set. Pass -AndroidSdkRoot explicitly.'
    }

    $adb = Join-Path $SdkRoot 'platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adb)) {
        Fail "adb.exe not found: $adb"
    }

    return $adb
}

function Invoke-Adb {
    param(
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $output = & $script:AdbPath @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    if (-not $AllowFailure -and $exitCode -ne 0) {
        $rendered = ($output | Out-String).Trim()
        Fail "adb failed ($exitCode): $($Arguments -join ' ')`n$rendered"
    }

    return [pscustomobject]@{
        ExitCode = $exitCode
        Output = ($output | Out-String).Trim()
    }
}

function Invoke-AdbShell {
    param(
        [string]$Command,
        [switch]$AllowFailure
    )

    return Invoke-Adb -Arguments @('-s', $DeviceSerial, 'shell', $Command) -AllowFailure:$AllowFailure
}

function Invoke-BrowserCommand {
    param([string]$Command)

    $wrapped = "run-as $AppPackage /system/bin/sh -c '$Command'"
    $result = Invoke-AdbShell -Command $wrapped
    $raw = $result.Output.Trim()
    if ([string]::IsNullOrWhiteSpace($raw)) {
        Fail "Browser command returned empty output: $Command"
    }

    try {
        return $raw | ConvertFrom-Json -Depth 20
    } catch {
        Fail "Failed to parse browser command JSON for: $Command`n$raw"
    }
}

function Assert-BrowserOk {
    param(
        [object]$Payload,
        [string]$Context
    )

    if (-not $Payload.ok) {
        $errorCode = $Payload.error.code
        $errorMessage = $Payload.error.message
        Fail "$Context failed: $errorCode - $errorMessage"
    }
}

function Wait-AppForeground {
    param([int]$TimeoutSeconds = 15)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $activityDump = Invoke-AdbShell -Command 'dumpsys activity activities'
        $activityPattern = [regex]::Escape($script:AppActivityComponent).Replace('/', '\/')
        if ($activityDump.Output -match "topResumedActivity=.*$activityPattern") {
            return
        }
        Start-Sleep -Milliseconds 500
    }

    Fail "Target activity did not reach foreground in time: $script:AppActivityComponent"
}

function Get-DebugApkPath {
    $apkName = if ($DebugApkVariant -eq 'universal') {
        'termux-app_apt-android-7-debug_universal.apk'
    } else {
        "termux-app_apt-android-7-debug_$DebugApkVariant.apk"
    }

    $apkPath = Join-Path $AppProjectDir "app\build\outputs\apk\debug\$apkName"
    if (-not (Test-Path -LiteralPath $apkPath)) {
        Fail "Debug APK not found: $apkPath"
    }

    return $apkPath
}

$script:AdbPath = Get-AdbPath -SdkRoot $AndroidSdkRoot
$AppProjectDir = (Resolve-Path $AppProjectDir).Path

Write-Step 'Checking device connection'
$devices = Invoke-Adb -Arguments @('devices', '-l')
Assert-Contains -Value $devices.Output -Expected $DeviceSerial -Message 'Device is not visible in adb devices output.'

if ($Build) {
    Write-Step 'Building debug APK'
    Push-Location $AppProjectDir
    try {
        $env:GRADLE_USER_HOME = Join-Path $AppProjectDir '.gradle'
        & .\gradlew.bat :app:assembleDebug
        if ($LASTEXITCODE -ne 0) {
            Fail 'Gradle debug build failed.'
        }
    } finally {
        Pop-Location
    }
}

if ($Install) {
    Write-Step 'Installing debug APK'
    $apkPath = Get-DebugApkPath
    $installResult = Invoke-Adb -Arguments @('-s', $DeviceSerial, 'install', '-r', $apkPath)
    Assert-Contains -Value $installResult.Output -Expected 'Success' -Message 'APK install did not report Success.'
}

Write-Step "Bringing target app to foreground ($script:AppActivityComponent)"
[void](Invoke-Adb -Arguments @('-s', $DeviceSerial, 'shell', 'am', 'start', '-n', $script:AppActivityComponent))
Wait-AppForeground
Start-Sleep -Seconds 2

Write-Step 'Smoke 1: daily read-console'
$consoleResult = Invoke-BrowserCommand "$script:BrowserCliPath read-console"
Assert-BrowserOk -Payload $consoleResult -Context 'daily read-console'

Write-Step 'Smoke 2: daily open example.com'
$dailyOpen = Invoke-BrowserCommand "$script:BrowserCliPath open https://example.com"
Assert-BrowserOk -Payload $dailyOpen -Context 'daily open'
Assert-Contains -Value $dailyOpen.result.url -Expected 'https://example.com/' -Message 'daily open did not end on example.com.'

$dailyReadText = Invoke-BrowserCommand "$script:BrowserCliPath read-text"
Assert-BrowserOk -Payload $dailyReadText -Context 'daily read-text'
Assert-Contains -Value $dailyReadText.result.text -Expected 'Example Domain' -Message 'daily read-text did not contain expected page text.'

Write-Step 'Smoke 3: dev workspace form flow'
$devOpen = Invoke-BrowserCommand "$script:BrowserCliPath open https://httpbin.org/forms/post --mode dev --target-context workspace_webview --session-mode shared"
Assert-BrowserOk -Payload $devOpen -Context 'dev workspace open'
Assert-AnyContains -Value $devOpen.result.url -ExpectedValues @('https://httpbin.org/forms/post', 'https://httpbin.org/post') -Message 'dev workspace open did not land on the expected httpbin workflow page.'

$devReadTextInitial = Invoke-BrowserCommand "$script:BrowserCliPath read-text --mode dev --target-context workspace_webview --session-mode shared"
Assert-BrowserOk -Payload $devReadTextInitial -Context 'dev workspace initial read-text'
Assert-Contains -Value $devReadTextInitial.result.url -Expected 'https://httpbin.org/forms/post' -Message 'dev workspace did not stay on the form page before interactive actions.'
Assert-Contains -Value $devReadTextInitial.result.text -Expected 'Customer name:' -Message 'dev workspace initial page text did not match the expected form.'

$strictInteractivePassed = $null
if ($StrictInteractive) {
    $devType = Invoke-BrowserCommand "$script:BrowserCliPath type --selector input[name=custname] --text smoke-script --mode dev --target-context workspace_webview --session-mode shared"
    Assert-BrowserOk -Payload $devType -Context 'dev workspace type'
    Assert-Contains -Value $devType.result.value -Expected 'smoke-script' -Message 'dev workspace type did not persist the typed value.'

    $devClickRadio = Invoke-BrowserCommand "$script:BrowserCliPath click --selector input[name=size][value=medium] --mode dev --target-context workspace_webview --session-mode shared"
    Assert-BrowserOk -Payload $devClickRadio -Context 'dev workspace click radio'

    $devSubmit = Invoke-BrowserCommand "$script:BrowserCliPath click --selector button --mode dev --target-context workspace_webview --session-mode shared"
    Assert-BrowserOk -Payload $devSubmit -Context 'dev workspace submit'

    $devReadText = Invoke-BrowserCommand "$script:BrowserCliPath read-text --mode dev --target-context workspace_webview --session-mode shared"
    Assert-BrowserOk -Payload $devReadText -Context 'dev workspace read-text'
    Assert-Contains -Value $devReadText.result.url -Expected 'https://httpbin.org/post' -Message 'dev workspace form submit did not navigate to the result page.'
    Assert-Contains -Value $devReadText.result.text -Expected '"custname": "smoke-script"' -Message 'dev workspace result did not include the typed customer name.'
    Assert-Contains -Value $devReadText.result.text -Expected '"size": "medium"' -Message 'dev workspace result did not include the selected size.'

    $devReadDom = Invoke-BrowserCommand "$script:BrowserCliPath read-dom --mode dev --target-context workspace_webview --session-mode shared"
    Assert-BrowserOk -Payload $devReadDom -Context 'dev workspace read-dom'
    Assert-Contains -Value $devReadDom.result.url -Expected 'https://httpbin.org/post' -Message 'dev workspace read-dom did not stay on the submitted page.'
    $strictInteractivePassed = $true
} else {
    $strictInteractivePassed = $false
}

Write-Step 'Smoke complete'
[pscustomobject]@{
    device = $DeviceSerial
    daily_url = $dailyOpen.result.url
    dev_url = $devReadTextInitial.result.url
    strict_interactive = $strictInteractivePassed
} | ConvertTo-Json
