package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.config.ModeConfig;
import com.termux.app.config.ModeConfigManager;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class BrowserCommandRequest {

    @NonNull public final String requestId;
    @NonNull public final String action;
    @Nullable public final String url;
    @Nullable public final String selector;
    @Nullable public final String text;
    @Nullable public final String output;
    @Nullable public final String taskDefault;
    public final boolean targetContextExplicit;
    @NonNull public final String mode;
    @NonNull public final String sessionMode;
    @Nullable public final String sessionProfile;
    @Nullable public final String shareFrom;
    @NonNull public final String targetContext;
    @NonNull public final String resultDelivery;
    public final int deltaY;
    public final long timeoutMs;

    private BrowserCommandRequest(@NonNull String requestId, @NonNull String action,
                                  @Nullable String url, @Nullable String selector,
                                  @Nullable String text, @Nullable String output,
                                  @Nullable String taskDefault,
                                  boolean targetContextExplicit, @NonNull String mode,
                                  @NonNull String sessionMode, @Nullable String sessionProfile,
                                  @Nullable String shareFrom, @NonNull String targetContext,
                                  @NonNull String resultDelivery, int deltaY, long timeoutMs) {
        this.requestId = requestId;
        this.action = action;
        this.url = url;
        this.selector = selector;
        this.text = text;
        this.output = output;
        this.taskDefault = taskDefault;
        this.targetContextExplicit = targetContextExplicit;
        this.mode = mode;
        this.sessionMode = sessionMode;
        this.sessionProfile = sessionProfile;
        this.shareFrom = shareFrom;
        this.targetContext = targetContext;
        this.resultDelivery = resultDelivery;
        this.deltaY = deltaY;
        this.timeoutMs = timeoutMs;
    }

    @NonNull
    public static BrowserCommandRequest fromRequestId(@NonNull String requestId) throws IllegalArgumentException {
        if (!TermuxBrowserWorkspace.isValidRequestId(requestId)) {
            throw new IllegalArgumentException("Invalid request id.");
        }

        String requestDirPath = TermuxBrowserWorkspace.getRequestDirectoryPath(requestId);
        ModeConfig modeConfig = ModeConfigManager.getModeConfig();
        String defaultMode = modeConfig.getDefaultMode();
        String action = readRequiredFile(requestDirPath, "action");
        String url = readOptionalFile(requestDirPath, "url");
        String selector = readOptionalFile(requestDirPath, "selector");
        String text = readOptionalFile(requestDirPath, "text");
        String output = readOptionalFile(requestDirPath, "output");
        String taskDefault = emptyToNull(readOptionalFile(requestDirPath, "task_default"));
        JSONObject taskDefaultObject = null;
        if (taskDefault != null) {
            taskDefaultObject = modeConfig.getTaskDefault(taskDefault);
            if (taskDefaultObject == null) {
                throw new IllegalArgumentException("Unknown task default: " + taskDefault);
            }
        }

        String modeValue = readOptionalFile(requestDirPath, "mode");
        boolean modeExplicit = isExplicit(readOptionalFile(requestDirPath, "mode_explicit"));
        String mode = modeExplicit ?
            defaultString(modeValue, defaultMode) :
            defaultString(optString(taskDefaultObject, "mode"), defaultString(modeValue, defaultMode));

        String sessionModeValue = readOptionalFile(requestDirPath, "session_mode");
        boolean sessionModeExplicit = isExplicit(readOptionalFile(requestDirPath, "session_mode_explicit"));
        String sessionMode = sessionModeExplicit ?
            defaultString(sessionModeValue, modeConfig.getDefaultSessionMode(mode)) :
            defaultString(optString(taskDefaultObject, "session_mode"),
                defaultString(sessionModeValue, modeConfig.getDefaultSessionMode(mode)));

        String sessionProfile = defaultNullable(readOptionalFile(requestDirPath, "session_profile"),
            optString(taskDefaultObject, "session_profile"));
        String shareFrom = defaultNullable(readOptionalFile(requestDirPath, "share_from"),
            optString(taskDefaultObject, "share_from"));
        String targetContextValue = readOptionalFile(requestDirPath, "target_context");
        boolean targetContextExplicit = isExplicit(readOptionalFile(requestDirPath, "target_context_explicit")) ||
            emptyToNull(targetContextValue) != null;
        String targetContext = targetContextExplicit ?
            defaultString(targetContextValue, modeConfig.getDefaultTargetContext(mode)) :
            defaultString(optString(taskDefaultObject, "target_context"),
                defaultString(targetContextValue, modeConfig.getDefaultTargetContext(mode)));

        String resultDeliveryValue = readOptionalFile(requestDirPath, "result_delivery");
        boolean resultDeliveryExplicit = isExplicit(readOptionalFile(requestDirPath, "result_delivery_explicit"));
        String resultDelivery = resultDeliveryExplicit ?
            defaultString(resultDeliveryValue, String.join(",", modeConfig.getDefaultResultDelivery(mode))) :
            defaultString(csvString(taskDefaultObject, "result_delivery"),
                defaultString(resultDeliveryValue, String.join(",", modeConfig.getDefaultResultDelivery(mode))));
        int deltaY = parseInt(readOptionalFile(requestDirPath, "delta_y"), 640);
        long timeoutMs = parseLong(readOptionalFile(requestDirPath, "timeout_ms"), 10000L);

        return new BrowserCommandRequest(requestId, action.trim(), emptyToNull(url), emptyToNull(selector),
            emptyToNull(text), emptyToNull(output), taskDefault, targetContextExplicit, mode, sessionMode,
            sessionProfile, shareFrom, targetContext, resultDelivery, deltaY, timeoutMs);
    }

    @NonNull
    private static String readRequiredFile(@NonNull String requestDirPath, @NonNull String name) {
        String value = readOptionalFile(requestDirPath, name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing request field: " + name);
        }

        return value;
    }

    @Nullable
    private static String readOptionalFile(@NonNull String requestDirPath, @NonNull String name) {
        StringBuilder data = new StringBuilder();
        Error error = FileUtils.readTextFromFile(name, requestDirPath + "/" + name, StandardCharsets.UTF_8, data, true);
        if (error != null) return null;
        return data.toString();
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        if (value == null) return null;
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private static int parseInt(@Nullable String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long parseLong(@Nullable String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @NonNull
    private static String defaultString(@Nullable String value, @NonNull String defaultValue) {
        value = emptyToNull(value);
        return value == null ? defaultValue : value;
    }

    @Nullable
    private static String defaultNullable(@Nullable String value, @Nullable String defaultValue) {
        value = emptyToNull(value);
        return value == null ? emptyToNull(defaultValue) : value;
    }

    @Nullable
    private static String optString(@Nullable JSONObject object, @NonNull String key) {
        if (object == null) return null;
        return emptyToNull(object.optString(key, null));
    }

    @Nullable
    private static String csvString(@Nullable JSONObject object, @NonNull String key) {
        if (object == null) return null;
        JSONArray array = object.optJSONArray(key);
        if (array == null || array.length() == 0) {
            return emptyToNull(object.optString(key, null));
        }

        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            String value = emptyToNull(array.optString(i, null));
            if (value == null) continue;
            if (csv.length() > 0) csv.append(',');
            csv.append(value);
        }

        return csv.length() == 0 ? null : csv.toString();
    }

    private static boolean isExplicit(@Nullable String value) {
        value = emptyToNull(value);
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

}
