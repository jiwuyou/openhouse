package com.termux.app.config;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.settings.properties.SharedProperties;
import com.termux.shared.termux.TermuxConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.charset.StandardCharsets;
import java.io.File;
import java.util.List;

public final class ModeConfigManager {

    private static final String LOG_TAG = "ModeConfigManager";

    @Nullable private static ModeConfig sModeConfig;

    private ModeConfigManager() {}

    @NonNull
    public static synchronized ModeConfig init(@NonNull Context context) {
        if (sModeConfig == null) {
            ensureConfigDirectory();
            sModeConfig = loadModeConfig();
            ensureExampleConfigFile(sModeConfig);
        }

        Logger.logInfo(LOG_TAG, "Mode config loaded: source=" + sModeConfig.getSourcePath() +
            ", fromDisk=" + sModeConfig.isLoadedFromDisk() +
            ", defaultMode=" + sModeConfig.getDefaultMode());

        return sModeConfig;
    }

    @NonNull
    public static synchronized ModeConfig getModeConfig() {
        if (sModeConfig == null) {
            throw new IllegalStateException("ModeConfigManager has not been initialized.");
        }

        return sModeConfig;
    }

    @NonNull
    private static ModeConfig loadModeConfig() {
        List<String> paths = TermuxConstants.TERMUX_MODE_CONFIG_FILE_PATHS_LIST;
        File file = SharedProperties.getPropertiesFileFromList(paths, LOG_TAG);
        if (file == null) {
            return new ModeConfig("(default)", false, "daily", buildDefaultConfigJson());
        }

        StringBuilder data = new StringBuilder();
        Error error = FileUtils.readTextFromFile("mode config", file.getAbsolutePath(), StandardCharsets.UTF_8, data, false);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
            return new ModeConfig(file.getAbsolutePath(), false, "daily", buildDefaultConfigJson());
        }

        try {
            JSONObject root = new JSONObject(new JSONTokener(data.toString()));
            String defaultMode = root.optString("default_mode", "daily");
            return new ModeConfig(file.getAbsolutePath(), true, defaultMode, root);
        } catch (JSONException e) {
            Logger.logErrorExtended(LOG_TAG, "Failed to parse mode config JSON\n" + e);
            return new ModeConfig(file.getAbsolutePath(), false, "daily", buildDefaultConfigJson());
        }
    }

    @NonNull
    private static JSONObject buildDefaultConfigJson() {
        JSONObject root = new JSONObject();
        JSONObject modes = new JSONObject();
        JSONObject daily = new JSONObject();
        JSONObject dev = new JSONObject();
        JSONObject automation = new JSONObject();
        JSONObject sessionProfiles = new JSONObject();
        JSONObject dailyShared = new JSONObject();
        JSONObject devShared = new JSONObject();
        JSONObject automationIsolated = new JSONObject();
        JSONObject taskDefaults = new JSONObject();
        JSONObject openUrl = new JSONObject();
        JSONObject dailyBackgroundJob = new JSONObject();
        JSONObject dailyBackgroundJobSharedLogin = new JSONObject();

        try {
            root.put("version", 1);
            root.put("default_mode", "daily");

            daily.put("enabled", true);
            daily.put("browser_runtime", "visible");
            daily.put("layout", "single_webview");
            daily.put("agent_panel", "control_webview");
            daily.put("terminal_panel", false);
            daily.put("workspace_webview", false);
            daily.put("allow_background_automation", true);
            daily.put("default_session_mode", "shared");
            daily.put("default_target_context", "control_webview");
            daily.put("default_result_delivery", new JSONArray().put("notification").put("control_webview"));

            dev.put("enabled", true);
            dev.put("browser_runtime", "visible");
            dev.put("layout", "three_panel");
            dev.put("agent_panel", "control_webview");
            dev.put("terminal_panel", true);
            dev.put("workspace_webview", true);
            dev.put("allow_background_automation", true);
            dev.put("default_session_mode", "shared");
            dev.put("default_target_context", "workspace_webview");
            dev.put("default_result_delivery", new JSONArray().put("workspace_webview").put("terminal"));

            automation.put("enabled", true);
            automation.put("browser_runtime", "headless");
            automation.put("layout", "none");
            automation.put("agent_panel", "none");
            automation.put("terminal_panel", false);
            automation.put("workspace_webview", false);
            automation.put("allow_background_automation", true);
            automation.put("default_session_mode", "isolated");
            automation.put("default_target_context", "headless");
            automation.put("default_result_delivery", new JSONArray().put("terminal").put("notification"));

            modes.put("daily", daily);
            modes.put("dev", dev);
            modes.put("automation", automation);

            root.put("modes", modes);

            dailyShared.put("cookie_scope", "daily");
            dailyShared.put("storage_scope", "daily");
            dailyShared.put("login_state", "shared");

            devShared.put("cookie_scope", "dev");
            devShared.put("storage_scope", "dev");
            devShared.put("login_state", "shared");

            automationIsolated.put("cookie_scope", "isolated");
            automationIsolated.put("storage_scope", "isolated");
            automationIsolated.put("login_state", "isolated");

            sessionProfiles.put("daily_shared", dailyShared);
            sessionProfiles.put("dev_shared", devShared);
            sessionProfiles.put("automation_isolated", automationIsolated);

            openUrl.put("mode", "dev");
            openUrl.put("session_mode", "shared");
            openUrl.put("target_context", "workspace_webview");
            openUrl.put("result_delivery", new JSONArray().put("workspace_webview").put("terminal"));

            dailyBackgroundJob.put("mode", "automation");
            dailyBackgroundJob.put("session_mode", "isolated");
            dailyBackgroundJob.put("target_context", "headless");
            dailyBackgroundJob.put("result_delivery", new JSONArray().put("notification"));

            dailyBackgroundJobSharedLogin.put("mode", "automation");
            dailyBackgroundJobSharedLogin.put("session_mode", "shared");
            dailyBackgroundJobSharedLogin.put("share_from", "daily");
            dailyBackgroundJobSharedLogin.put("target_context", "headless");
            dailyBackgroundJobSharedLogin.put("result_delivery", new JSONArray().put("notification").put("control_webview"));

            taskDefaults.put("open_url", openUrl);
            taskDefaults.put("daily_background_job", dailyBackgroundJob);
            taskDefaults.put("daily_background_job_shared_login", dailyBackgroundJobSharedLogin);

            root.put("session_profiles", sessionProfiles);
            root.put("task_defaults", taskDefaults);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }

        return root;
    }

    private static void ensureConfigDirectory() {
        Error error = FileUtils.createDirectoryFile("termux config directory",
            TermuxConstants.TERMUX_CONFIG_HOME_DIR_PATH, "rwx", true, true);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

    private static void ensureExampleConfigFile(@NonNull ModeConfig modeConfig) {
        if (FileUtils.fileExists(TermuxConstants.TERMUX_MODE_CONFIG_EXAMPLE_FILE_PATH, false)) return;

        String jsonString;
        try {
            jsonString = modeConfig.getRoot().toString(2) + "\n";
        } catch (JSONException e) {
            Logger.logErrorExtended(LOG_TAG, "Failed to serialize mode config example\n" + e);
            jsonString = modeConfig.getRoot().toString() + "\n";
        }

        Error error = FileUtils.writeTextToFile("mode config example",
            TermuxConstants.TERMUX_MODE_CONFIG_EXAMPLE_FILE_PATH,
            StandardCharsets.UTF_8,
            jsonString,
            false);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

}
