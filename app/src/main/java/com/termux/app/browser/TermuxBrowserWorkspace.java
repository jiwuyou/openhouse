package com.termux.app.browser;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.system.Os;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.TermuxActivity;
import com.termux.app.config.ModeConfig;
import com.termux.app.config.ModeConfigManager;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.regex.Pattern;

public final class TermuxBrowserWorkspace {

    private static final String LOG_TAG = "TermuxBrowserWorkspace";
    private static final String CLI_SCRIPT_VERSION = "7";
    private static final String HOST_BRIDGE_SCRIPT_VERSION = "1";
    private static final String WORKSPACE_PREVIEW_SCHEME = "https";
    private static final String WORKSPACE_PREVIEW_HOST = "termux.local";
    private static final String WORKSPACE_PREVIEW_PREFIX = "/workspace/";
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[0-9A-Za-z._-]+$");
    public static final String EXTRA_TARGET_MODE = "com.termux.browser.extra.TARGET_MODE";

    private TermuxBrowserWorkspace() {}

    public static void ensureRuntimeSetup(@NonNull Context context) {
        ensureDirectory(TermuxConstants.TERMUX_APP.TERMUX_BROWSER_APPS_DIR_PATH);
        ensureDirectory(TermuxConstants.TERMUX_APP.TERMUX_BROWSER_REQUESTS_DIR_PATH);
        ensureDirectory(TermuxConstants.TERMUX_APP.TERMUX_BROWSER_RESPONSES_DIR_PATH);
        ensureDirectory(TermuxConstants.TERMUX_APP.TERMUX_BROWSER_DOWNLOADS_DIR_PATH);

        if (isPrefixUsable()) {
            installBrowserCli();
            installHostBridgeCli();
        }
    }

    private static boolean isPrefixUsable() {
        return new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "login").exists() ||
            new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "sh").exists();
    }

    public static void launchTermuxActivity(@NonNull Context context, @Nullable String requestedMode) {
        Intent intent = new Intent(context, TermuxActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (requestedMode != null && !requestedMode.trim().isEmpty()) {
            intent.putExtra(EXTRA_TARGET_MODE, requestedMode);
        }
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Logger.logWarnExtended(LOG_TAG, "Failed to foreground TermuxActivity\n" + e);
        }
    }

    @NonNull
    public static String getRequestDirectoryPath(@NonNull String requestId) {
        return TermuxConstants.TERMUX_APP.TERMUX_BROWSER_REQUESTS_DIR_PATH + "/" + requestId;
    }

    @NonNull
    public static String getResponseDirectoryPath(@NonNull String requestId) {
        return TermuxConstants.TERMUX_APP.TERMUX_BROWSER_RESPONSES_DIR_PATH + "/" + requestId;
    }

    public static boolean isValidRequestId(@Nullable String requestId) {
        return requestId != null && REQUEST_ID_PATTERN.matcher(requestId).matches();
    }

    public static boolean requestDirectoryExists(@NonNull String requestId) {
        return isValidRequestId(requestId) &&
            FileUtils.directoryFileExists(getRequestDirectoryPath(requestId), false);
    }

    public static void writeResponse(@NonNull String requestId, @NonNull BrowserCommandResult result) {
        String responseDir = getResponseDirectoryPath(requestId);
        ensureDirectory(responseDir);
        writeText(responseDir + "/status", String.valueOf(result.exitCode) + "\n");
        writeText(responseDir + "/result.json", result.payload.toString() + "\n");
    }

    public static void appendDebugLog(@NonNull String requestId, @NonNull String line) {
        String responseDir = getResponseDirectoryPath(requestId);
        ensureDirectory(responseDir);
        appendText(responseDir + "/debug.log", line + "\n");
    }

    public static void writeResponseError(@NonNull String requestId, @NonNull String action,
                                          @NonNull String code, @NonNull String message) {
        writeResponse(requestId, BrowserCommandResult.error(action, code, message));
    }

    @NonNull
    public static Uri getWorkspaceDocumentsUri() {
        return DocumentsContract.buildDocumentUri(
            TermuxConstants.TERMUX_PACKAGE_NAME + ".documents",
            TermuxConstants.TERMUX_HOME_DIR_PATH
        );
    }

    public static boolean isWorkspaceFilePath(@NonNull String path) {
        try {
            String canonicalPath = new File(path).getCanonicalPath();
            String workspaceCanonical = new File(TermuxConstants.TERMUX_APP.TERMUX_BROWSER_WORKSPACE_DIR_PATH).getCanonicalPath();
            return canonicalPath.equals(workspaceCanonical) || canonicalPath.startsWith(workspaceCanonical + File.separator);
        } catch (Exception e) {
            return false;
        }
    }

    @NonNull
    public static String toWorkspacePreviewUrl(@NonNull String path) {
        try {
            File workspaceRoot = new File(TermuxConstants.TERMUX_APP.TERMUX_BROWSER_WORKSPACE_DIR_PATH);
            String workspaceCanonical = workspaceRoot.getCanonicalPath();
            String fileCanonical = new File(path).getCanonicalPath();
            String relativePath = fileCanonical.equals(workspaceCanonical) ? "" :
                fileCanonical.substring(workspaceCanonical.length()).replace(File.separatorChar, '/');
            if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
            return WORKSPACE_PREVIEW_SCHEME + "://" + WORKSPACE_PREVIEW_HOST + WORKSPACE_PREVIEW_PREFIX + relativePath;
        } catch (Exception e) {
            return path;
        }
    }

    @Nullable
    public static File resolveWorkspacePreviewFile(@NonNull Uri uri) {
        if (!WORKSPACE_PREVIEW_SCHEME.equals(uri.getScheme())) return null;
        if (!WORKSPACE_PREVIEW_HOST.equals(uri.getHost())) return null;

        String path = uri.getPath();
        if (path == null || !path.startsWith(WORKSPACE_PREVIEW_PREFIX)) return null;

        String relativePath = path.substring(WORKSPACE_PREVIEW_PREFIX.length());
        if (relativePath.isEmpty()) return null;

        try {
            relativePath = URLDecoder.decode(relativePath, "UTF-8");
            File workspaceRoot = new File(TermuxConstants.TERMUX_APP.TERMUX_BROWSER_WORKSPACE_DIR_PATH);
            File targetFile = new File(workspaceRoot, relativePath);
            String workspaceCanonical = workspaceRoot.getCanonicalPath();
            String targetCanonical = targetFile.getCanonicalPath();
            if (!(targetCanonical.equals(workspaceCanonical) || targetCanonical.startsWith(workspaceCanonical + File.separator))) {
                return null;
            }
            if (!targetFile.isFile()) return null;
            return targetFile;
        } catch (Exception e) {
            Logger.logErrorExtended(LOG_TAG, "Failed to resolve workspace preview file\n" + e);
            return null;
        }
    }

    @NonNull
    public static String getMimeType(@NonNull File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            String extension = name.substring(lastDot + 1).toLowerCase();
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) return mime;
        }
        return "application/octet-stream";
    }

    @Nullable
    public static android.webkit.WebResourceResponse openWorkspacePreviewResponse(@NonNull Uri uri) {
        File file = resolveWorkspacePreviewFile(uri);
        if (file == null) return null;

        try {
            return new android.webkit.WebResourceResponse(getMimeType(file), null, new FileInputStream(file));
        } catch (Exception e) {
            Logger.logErrorExtended(LOG_TAG, "Failed to open workspace preview response\n" + e);
            return null;
        }
    }

    @Nullable
    public static String readWorkspacePreviewText(@NonNull Uri uri) {
        File file = resolveWorkspacePreviewFile(uri);
        if (file == null) return null;

        String mimeType = getMimeType(file);
        if (!(mimeType.startsWith("text/") ||
            "application/javascript".equals(mimeType) ||
            "application/json".equals(mimeType))) {
            return null;
        }

        StringBuilder data = new StringBuilder();
        Error error = FileUtils.readTextFromFile("workspace preview", file.getAbsolutePath(), StandardCharsets.UTF_8, data, false);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
            return null;
        }

        return data.toString();
    }

    private static void installBrowserCli() {
        String scriptPath = TermuxConstants.TERMUX_APP.TERMUX_BROWSER_CLI_PATH;
        String script = buildCliScript();
        String existing = readText(scriptPath);
        if (script.equals(existing)) {
            return;
        }

        writeText(scriptPath, script);
        try {
            Os.chmod(scriptPath, 0700);
            Logger.logInfo(LOG_TAG, "Updated termux-browser CLI to version " + CLI_SCRIPT_VERSION);
        } catch (Exception e) {
            Logger.logErrorExtended(LOG_TAG, "Failed to chmod browser cli\n" + e);
        }
    }

    private static void installHostBridgeCli() {
        String scriptPath = TermuxConstants.TERMUX_APP.TERMUX_HOST_BRIDGE_CLI_PATH;
        String script = buildHostBridgeScript();
        String existing = readText(scriptPath);
        if (script.equals(existing)) {
            return;
        }

        writeText(scriptPath, script);
        try {
            Os.chmod(scriptPath, 0700);
            Logger.logInfo(LOG_TAG, "Updated termux-host bridge CLI to version " + HOST_BRIDGE_SCRIPT_VERSION);
        } catch (Exception e) {
            Logger.logErrorExtended(LOG_TAG, "Failed to chmod host bridge cli\n" + e);
        }
    }

    private static void ensureDirectory(@NonNull String path) {
        Error error = FileUtils.createDirectoryFile(path, path, "rwx", true, true);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

    private static void writeText(@NonNull String path, @NonNull String text) {
        Error error = FileUtils.writeTextToFile(path, path, StandardCharsets.UTF_8, text, false);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

    private static void appendText(@NonNull String path, @NonNull String text) {
        Error error = FileUtils.writeTextToFile(path, path, StandardCharsets.UTF_8, text, true);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

    @Nullable
    private static String readText(@NonNull String path) {
        StringBuilder data = new StringBuilder();
        Error error = FileUtils.readTextFromFile(path, path, StandardCharsets.UTF_8, data, true);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
            return null;
        }

        if (data.length() == 0) return null;
        return data.toString();
    }

    @NonNull
    private static String buildCliScript() {
        String cliName = "termux-browser";
        ModeConfig modeConfig = ModeConfigManager.getModeConfig();
        String defaultMode = modeConfig.getDefaultMode();
        String defaultSessionMode = modeConfig.getDefaultSessionMode(defaultMode);
        String defaultTargetContext = modeConfig.getDefaultTargetContext(defaultMode);
        String defaultResultDelivery = String.join(",", modeConfig.getDefaultResultDelivery(defaultMode));
        return "#!/system/bin/sh\n" +
            "# termux-browser-version:" + CLI_SCRIPT_VERSION + "\n" +
            "set -eu\n" +
            "usage() {\n" +
            "  cat <<'EOF'\n" +
            "Usage:\n" +
            "  " + cliName + " open <url> [--timeout-ms N] [--task-default NAME] [--mode NAME] [--session-mode MODE] [--session-profile NAME] [--share-from SRC] [--target-context CTX] [--result-delivery DESTS]\n" +
            "  " + cliName + " click --selector <css> [--timeout-ms N] [--task-default NAME] [--mode NAME] [--session-mode MODE] [--session-profile NAME] [--share-from SRC] [--target-context CTX] [--result-delivery DESTS]\n" +
            "  " + cliName + " type --selector <css> --text <value> [--timeout-ms N] [--task-default NAME] [--mode NAME] [--session-mode MODE] [--session-profile NAME] [--share-from SRC] [--target-context CTX] [--result-delivery DESTS]\n" +
            "  " + cliName + " scroll [--delta-y N] [--timeout-ms N] [--task-default NAME] [--mode NAME] [--session-mode MODE] [--session-profile NAME] [--share-from SRC] [--target-context CTX] [--result-delivery DESTS]\n" +
            "  " + cliName + " read-text [--timeout-ms N] [--task-default NAME] [--mode NAME] [--session-mode MODE] [--session-profile NAME] [--share-from SRC] [--target-context CTX] [--result-delivery DESTS]\n" +
            "  " + cliName + " read-dom [--timeout-ms N] [--task-default NAME] [--mode NAME] [--session-mode MODE] [--session-profile NAME] [--share-from SRC] [--target-context CTX] [--result-delivery DESTS]\n" +
            "  " + cliName + " read-console [--task-default NAME] [--mode NAME] [--session-mode MODE] [--session-profile NAME] [--share-from SRC] [--target-context CTX] [--result-delivery DESTS]\n" +
            "  " + cliName + " screenshot [--output PATH] [--timeout-ms N] [--task-default NAME] [--mode NAME] [--session-mode MODE] [--session-profile NAME] [--share-from SRC] [--target-context CTX] [--result-delivery DESTS]\n" +
            "EOF\n" +
            "}\n" +
            "ACTION=\"${1:-}\"\n" +
            "[ -n \"$ACTION\" ] || { usage >&2; exit 2; }\n" +
            "shift || true\n" +
            "REQUEST_ID=\"$(date +%s%N)-$$\"\n" +
            "REQUEST_DIR=\"" + TermuxConstants.TERMUX_APP.TERMUX_BROWSER_REQUESTS_DIR_PATH + "/$REQUEST_ID\"\n" +
            "RESPONSE_DIR=\"" + TermuxConstants.TERMUX_APP.TERMUX_BROWSER_RESPONSES_DIR_PATH + "/$REQUEST_ID\"\n" +
            "mkdir -p \"$REQUEST_DIR\" \"$RESPONSE_DIR\"\n" +
            "cleanup() {\n" +
            "  rm -rf \"$REQUEST_DIR\" \"$RESPONSE_DIR\"\n" +
            "}\n" +
            "trap cleanup EXIT INT TERM\n" +
            "write_value() {\n" +
            "  key=\"$1\"\n" +
            "  shift\n" +
            "  printf '%s' \"$1\" > \"$REQUEST_DIR/$key\"\n" +
            "}\n" +
            "TIMEOUT_MS=\"10000\"\n" +
            "MODE=\"" + defaultMode + "\"\n" +
            "SESSION_MODE=\"" + defaultSessionMode + "\"\n" +
            "SESSION_PROFILE=\"\"\n" +
            "TASK_DEFAULT=\"\"\n" +
            "SHARE_FROM=\"\"\n" +
            "TARGET_CONTEXT=\"" + defaultTargetContext + "\"\n" +
            "RESULT_DELIVERY=\"" + defaultResultDelivery + "\"\n" +
            "MODE_EXPLICIT=0\n" +
            "SESSION_MODE_EXPLICIT=0\n" +
            "TARGET_CONTEXT_EXPLICIT=0\n" +
            "RESULT_DELIVERY_EXPLICIT=0\n" +
            buildApplyModeDefaultsFunction(modeConfig) +
            "case \"$ACTION\" in\n" +
            "  open)\n" +
            "    [ $# -ge 1 ] || { usage >&2; exit 2; }\n" +
            "    write_value action open\n" +
            "    write_value url \"$1\"\n" +
            "    shift\n" +
            "    ;;\n" +
            "  click)\n" +
            "    write_value action click\n" +
            "    ;;\n" +
            "  type)\n" +
            "    write_value action type\n" +
            "    ;;\n" +
            "  scroll)\n" +
            "    write_value action scroll\n" +
            "    ;;\n" +
            "  read-text)\n" +
            "    write_value action read_text\n" +
            "    ;;\n" +
            "  read-dom)\n" +
            "    write_value action read_dom\n" +
            "    ;;\n" +
            "  read-console)\n" +
            "    write_value action read_console\n" +
            "    ;;\n" +
            "  screenshot)\n" +
            "    write_value action screenshot\n" +
            "    ;;\n" +
            "  *)\n" +
            "    usage >&2\n" +
            "    exit 2\n" +
            "    ;;\n" +
            "esac\n" +
            "while [ $# -gt 0 ]; do\n" +
            "  case \"$1\" in\n" +
            "    --selector)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; write_value selector \"$1\" ;;\n" +
            "    --text)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; write_value text \"$1\" ;;\n" +
            "    --output)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; write_value output \"$1\" ;;\n" +
            "    --delta-y)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; write_value delta_y \"$1\" ;;\n" +
            "    --timeout-ms)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; TIMEOUT_MS=\"$1\" ;;\n" +
            "    --mode)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; MODE=\"$1\"; MODE_EXPLICIT=1; apply_mode_defaults ;;\n" +
            "    --task-default)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; TASK_DEFAULT=\"$1\" ;;\n" +
            "    --session-mode)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; SESSION_MODE=\"$1\"; SESSION_MODE_EXPLICIT=1 ;;\n" +
            "    --session-profile)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; SESSION_PROFILE=\"$1\" ;;\n" +
            "    --share-from)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; SHARE_FROM=\"$1\" ;;\n" +
            "    --target-context)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; TARGET_CONTEXT=\"$1\"; TARGET_CONTEXT_EXPLICIT=1 ;;\n" +
            "    --result-delivery)\n" +
            "      shift; [ $# -gt 0 ] || exit 2; RESULT_DELIVERY=\"$1\"; RESULT_DELIVERY_EXPLICIT=1 ;;\n" +
            "    *)\n" +
            "      echo \"Unknown argument: $1\" >&2\n" +
            "      exit 2\n" +
            "      ;;\n" +
            "  esac\n" +
            "  shift\n" +
            "done\n" +
            "apply_mode_defaults\n" +
            "write_value timeout_ms \"$TIMEOUT_MS\"\n" +
            "[ -n \"$TASK_DEFAULT\" ] && write_value task_default \"$TASK_DEFAULT\"\n" +
            "[ -n \"$TASK_DEFAULT\" ] || write_value mode \"$MODE\"\n" +
            "[ \"$MODE_EXPLICIT\" -eq 1 ] && write_value mode_explicit 1\n" +
            "[ \"$MODE_EXPLICIT\" -eq 1 ] && write_value mode \"$MODE\"\n" +
            "[ -n \"$TASK_DEFAULT\" ] || write_value session_mode \"$SESSION_MODE\"\n" +
            "[ \"$SESSION_MODE_EXPLICIT\" -eq 1 ] && write_value session_mode_explicit 1\n" +
            "[ \"$SESSION_MODE_EXPLICIT\" -eq 1 ] && write_value session_mode \"$SESSION_MODE\"\n" +
            "[ -n \"$SESSION_PROFILE\" ] && write_value session_profile \"$SESSION_PROFILE\"\n" +
            "[ -n \"$SHARE_FROM\" ] && write_value share_from \"$SHARE_FROM\"\n" +
            "[ -n \"$TASK_DEFAULT\" ] || write_value target_context \"$TARGET_CONTEXT\"\n" +
            "[ \"$TARGET_CONTEXT_EXPLICIT\" -eq 1 ] && write_value target_context_explicit 1\n" +
            "[ \"$TARGET_CONTEXT_EXPLICIT\" -eq 1 ] && write_value target_context \"$TARGET_CONTEXT\"\n" +
            "[ -n \"$TASK_DEFAULT\" ] || write_value result_delivery \"$RESULT_DELIVERY\"\n" +
            "[ \"$RESULT_DELIVERY_EXPLICIT\" -eq 1 ] && write_value result_delivery_explicit 1\n" +
            "[ \"$RESULT_DELIVERY_EXPLICIT\" -eq 1 ] && write_value result_delivery \"$RESULT_DELIVERY\"\n" +
            "/system/bin/am start -W --user 0 -n " + TermuxConstants.TERMUX_PACKAGE_NAME + "/.app.TermuxActivity >/dev/null 2>&1 || true\n" +
            "/system/bin/am broadcast --user 0 -n " + TermuxConstants.TERMUX_PACKAGE_NAME + "/" + TermuxConstants.TERMUX_APP.BROWSER_COMMAND_RECEIVER_CLASS_NAME + " --es request_id \"$REQUEST_ID\" >/dev/null\n" +
            "STATUS_FILE=\"$RESPONSE_DIR/status\"\n" +
            "RESULT_FILE=\"$RESPONSE_DIR/result.json\"\n" +
            "i=0\n" +
            "while [ $i -lt 400 ]; do\n" +
            "  if [ -f \"$STATUS_FILE\" ] && [ -f \"$RESULT_FILE\" ]; then\n" +
            "    cat \"$RESULT_FILE\"\n" +
            "    exit \"$(cat \"$STATUS_FILE\")\"\n" +
            "  fi\n" +
            "  i=$((i + 1))\n" +
            "  sleep 0.05\n" +
            "done\n" +
            "printf '%s\\n' '{\"ok\":false,\"error\":{\"code\":\"TIMEOUT\",\"message\":\"Timed out waiting for browser command result.\"}}'\n" +
            "exit 124\n";
    }

    @NonNull
    private static String buildApplyModeDefaultsFunction(@NonNull ModeConfig modeConfig) {
        StringBuilder script = new StringBuilder();
        script.append("apply_mode_defaults() {\n");
        script.append("  case \"$MODE\" in\n");
        appendModeDefaults(script, "daily", modeConfig);
        appendModeDefaults(script, "dev", modeConfig);
        appendModeDefaults(script, "automation", modeConfig);
        script.append("    *)\n");
        script.append("      ;;\n");
        script.append("  esac\n");
        script.append("}\n");
        return script.toString();
    }

    private static void appendModeDefaults(@NonNull StringBuilder script,
                                           @NonNull String mode,
                                           @NonNull ModeConfig modeConfig) {
        script.append("    ").append(mode).append(")\n");
        script.append("      [ \"$SESSION_MODE_EXPLICIT\" -eq 1 ] || SESSION_MODE=\"")
            .append(modeConfig.getDefaultSessionMode(mode)).append("\"\n");
        script.append("      [ \"$TARGET_CONTEXT_EXPLICIT\" -eq 1 ] || TARGET_CONTEXT=\"")
            .append(modeConfig.getDefaultTargetContext(mode)).append("\"\n");
        script.append("      [ \"$RESULT_DELIVERY_EXPLICIT\" -eq 1 ] || RESULT_DELIVERY=\"")
            .append(String.join(",", modeConfig.getDefaultResultDelivery(mode))).append("\"\n");
        script.append("      ;;\n");
    }

    @NonNull
    private static String buildHostBridgeScript() {
        ModeConfig modeConfig = ModeConfigManager.getModeConfig();
        String defaultMode = modeConfig.getDefaultMode();
        return "#!/system/bin/sh\n" +
            "# termux-host-version:" + HOST_BRIDGE_SCRIPT_VERSION + "\n" +
            "set -eu\n" +
            "usage() {\n" +
            "  cat <<'EOF'\n" +
            "Usage:\n" +
            "  termux-host browser <termux-browser args...>\n" +
            "  termux-host paths\n" +
            "  termux-host mode show\n" +
            "EOF\n" +
            "}\n" +
            "CMD=\"${1:-}\"\n" +
            "[ -n \"$CMD\" ] || { usage >&2; exit 2; }\n" +
            "shift || true\n" +
            "case \"$CMD\" in\n" +
            "  browser)\n" +
            "    exec " + TermuxConstants.TERMUX_APP.TERMUX_BROWSER_CLI_PATH + " \"$@\"\n" +
            "    ;;\n" +
            "  paths)\n" +
            "    cat <<'EOF'\n" +
            "WORKSPACE=" + TermuxConstants.TERMUX_APP.TERMUX_BROWSER_WORKSPACE_DIR_PATH + "\n" +
            "DOWNLOADS=" + TermuxConstants.TERMUX_APP.TERMUX_BROWSER_DOWNLOADS_DIR_PATH + "\n" +
            "BROWSER_CLI=" + TermuxConstants.TERMUX_APP.TERMUX_BROWSER_CLI_PATH + "\n" +
            "HOST_BRIDGE_CLI=" + TermuxConstants.TERMUX_APP.TERMUX_HOST_BRIDGE_CLI_PATH + "\n" +
            "MODE_CONFIG_PRIMARY=" + TermuxConstants.TERMUX_MODE_CONFIG_PRIMARY_FILE_PATH + "\n" +
            "MODE_CONFIG_SECONDARY=" + TermuxConstants.TERMUX_MODE_CONFIG_SECONDARY_FILE_PATH + "\n" +
            "EOF\n" +
            "    ;;\n" +
            "  mode)\n" +
            "    SUBCMD=\"${1:-show}\"\n" +
            "    case \"$SUBCMD\" in\n" +
            "      show)\n" +
            "        cat <<'EOF'\n" +
            "default_mode=" + defaultMode + "\n" +
            "default_session_mode=" + modeConfig.getDefaultSessionMode(defaultMode) + "\n" +
            "default_target_context=" + modeConfig.getDefaultTargetContext(defaultMode) + "\n" +
            "default_result_delivery=" + String.join(",", modeConfig.getDefaultResultDelivery(defaultMode)) + "\n" +
            "EOF\n" +
            "        ;;\n" +
            "      *)\n" +
            "        usage >&2\n" +
            "        exit 2\n" +
            "        ;;\n" +
            "    esac\n" +
            "    ;;\n" +
            "  *)\n" +
            "    usage >&2\n" +
            "    exit 2\n" +
            "    ;;\n" +
            "esac\n";
    }

}
