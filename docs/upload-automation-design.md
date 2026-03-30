# Upload Automation Design

## 1. Document Status

- Status: Draft v0.1
- Date: 2026-03-25
- Language: zh-CN
- Related:
  - [architecture.md](/C:/Users/24045/termuweb/docs/architecture.md)
  - [stage-acceptance.md](/C:/Users/24045/termuweb/docs/stage-acceptance.md)
  - [release-scope.md](/C:/Users/24045/termuweb/docs/release-scope.md)
  - [current-progress-and-lessons-2026-03-25.md](/C:/Users/24045/termuweb/docs/current-progress-and-lessons-2026-03-25.md)

## 2. Purpose

本文档定义“网页上传文件”在长期架构中的正确实现方向。

结论先写在前面：

- 当前通过系统 `DocumentsUI` picker 完成上传，适合作为前台可见模式的过渡实现
- 但这不应成为 `automation` 模式的最终实现
- 当进入 `automation` 实现阶段时，应将上传链路升级为“同 app 内部文件直传”

## 3. Problem Statement

当前项目的上传链路本质上是：

1. 网页触发 `<input type="file">`
2. `WebView` 进入 `onShowFileChooser()`
3. App 启动系统文件选择器
4. 用户或自动化再去选文件

这条路径的问题不是“能不能用”，而是“它不适合作为 automation 的最终方案”。

原因：

- 文件和浏览器都在同一个 app 内
- 终端和浏览器共享同一个工作目录
- 自动化任务不应长期依赖系统 picker 这种前台交互
- 背景 automation 模式下，系统 picker 路径天然不稳定

## 4. Current State

当前已知状态：

- 在前台真机 smoke 中，系统文件选择器已经可以弹出
- 可以进入 `Termux` 提供的文件来源
- 可以从共享目录选文件并回传给网页

这说明：

- 当前方案足以支撑前台 `daily/dev` 的用户可见上传

但当前方案仍有明显限制：

- 上传成功依赖系统 `DocumentsUI`
- 自动化链路复杂
- 后台执行时不可依赖
- 文件选择动作不属于“同 app 内部直接传递”

## 5. Design Principle

最终应将上传能力拆成两类模式。

### 5.1 Manual Upload Mode

适用场景：

- `daily`
- `dev`
- 用户手动浏览

实现方式：

- 保留系统 picker
- 继续通过 `onShowFileChooser()` + `ACTION_OPEN_DOCUMENT`

特点：

- 符合 Android 用户预期
- 可见、直观、权限路径稳定

### 5.2 Automation Upload Mode

适用场景：

- `automation`
- Agent 驱动任务
- 后台 job
- 无人值守流程

实现方式：

- 不弹系统 picker
- 由 app 内部直接把共享目录文件转成可交给 WebView 的 URI
- 直接回填给 `ValueCallback<Uri[]>`

特点：

- 不依赖系统文件选择器
- 不依赖前台手动操作
- 更适合后台 automation runtime

## 6. Why Same-App Direct Injection Is Better

对 `automation` 而言，内部直传优于系统 picker，原因如下：

### 6.1 Same-App Resource Ownership

- 文件位于同 app 的共享工作目录
- 浏览器运行时也位于同 app 内
- 没有必要把“本应用文件”再通过“类外部选择器流程”转一遍

### 6.2 Better Automation Semantics

- Agent 只需要指定文件路径
- 不需要模拟打开 picker、浏览目录、点击文件
- 指令语义更清晰，也更稳定

### 6.3 Better Background Compatibility

- 系统 picker 偏前台交互
- automation runtime 需要后台可用
- 内部 URI 直传更适合后台执行

## 7. Target Architecture

进入 `automation` 阶段后，上传链路建议按以下模型实现。

### 7.1 Command Model

建议新增显式上传文件参数，例如：

```sh
termux-browser click --selector "input[type=file]" --upload-file /data/data/com.termux/files/home/foo.txt
```

或者：

```sh
termux-browser upload --selector "input[type=file]" --file /data/data/com.termux/files/home/foo.txt
```

关键点：

- Agent 不再“选文件”
- Agent 是“指定文件路径”

### 7.2 Browser Runtime Behavior

当网页触发 file chooser 时：

1. runtime 检查当前命令上下文是否带有上传文件参数
2. 若有：
   - 校验文件位于允许的共享目录内
   - 为该文件生成 app 内可读 `content://` URI
   - 直接调用 `ValueCallback<Uri[]>`
3. 若没有：
   - 回退到现有系统 picker 路径

### 7.3 File Exposure Layer

要求：

- 只允许共享目录内文件被自动化上传
- 不允许任意路径逃逸
- URI 生成应可被 WebView 安全读取

建议：

- 复用已有 `DocumentsProvider` 或新增专用 `FileProvider`
- 在 provider 层做路径白名单控制

## 8. Automation Runtime Dependency

这项能力不建议在当前前台浏览器实现里立即硬塞到底，原因是：

- 当前浏览器主要仍依赖前台 `Activity`
- automation 的长期方向是独立 runtime

因此推荐顺序是：

1. 先完成 automation runtime 边界定义
2. 再在 automation runtime 中加入文件上传直传能力
3. 最后决定是否把同样能力下沉复用给 `daily/dev`

## 9. Suggested Acceptance Criteria For Future Automation Upload

未来进入 automation 实现阶段后，这项能力建议按以下标准验收：

- Agent 可以通过命令直接指定共享目录内文件路径
- 网页文件输入框无需弹出系统 picker 即可获得文件
- 后台 automation 模式下可执行
- 非共享目录文件会被拒绝
- 回传错误语义清晰，例如：
  - `UPLOAD_FILE_NOT_FOUND`
  - `UPLOAD_FILE_OUTSIDE_WORKSPACE`
  - `UPLOAD_FILE_URI_CREATION_FAILED`
  - `UPLOAD_INPUT_NOT_FOUND`

## 10. Release Decision For Now

当前版本的发布策略建议保持如下：

- 前台 `daily/dev`：
  - 保留系统 picker 上传路径
- `automation`：
  - 暂不承诺内部直传上传
  - 将其列为后续实现项

这意味着：

- 当前上传链路可以算前台能力已通过
- 但“automation 上传直传”应作为后续阶段能力，不应假装已经完成

## 11. Recommended Follow-Up Work

进入 automation 阶段时，建议按以下顺序推进：

1. 定义 automation runtime 如何承载 file chooser 回调
2. 定义上传命令参数格式
3. 实现共享目录路径校验
4. 实现 app 内 URI 直传
5. 增加自动化上传验收用例
