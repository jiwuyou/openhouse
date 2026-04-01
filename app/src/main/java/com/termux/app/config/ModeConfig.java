package com.termux.app.config;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModeConfig {

    @NonNull private final String mSourcePath;
    private final boolean mLoadedFromDisk;
    @NonNull private final String mDefaultMode;
    @NonNull private final JSONObject mRoot;

    public ModeConfig(@NonNull String sourcePath, boolean loadedFromDisk,
                      @NonNull String defaultMode, @NonNull JSONObject root) {
        mSourcePath = sourcePath;
        mLoadedFromDisk = loadedFromDisk;
        mDefaultMode = defaultMode;
        mRoot = root;
    }

    @NonNull
    public String getSourcePath() {
        return mSourcePath;
    }

    public boolean isLoadedFromDisk() {
        return mLoadedFromDisk;
    }

    @NonNull
    public String getDefaultMode() {
        return mDefaultMode;
    }

    @NonNull
    public JSONObject getRoot() {
        return mRoot;
    }

    public boolean isModeEnabled(@NonNull String mode) {
        JSONObject modeObject = getModeObject(mode);
        return modeObject != null && modeObject.optBoolean("enabled", false);
    }

    @NonNull
    public String getBrowserRuntime(@NonNull String mode) {
        JSONObject modeObject = getModeObject(mode);
        if (modeObject == null) return "visible";
        return modeObject.optString("browser_runtime", "visible");
    }

    @NonNull
    public String getLayout(@NonNull String mode) {
        JSONObject modeObject = getModeObject(mode);
        if (modeObject == null) return "single_webview";
        return modeObject.optString("layout", "single_webview");
    }

    public boolean hasTerminalPanel(@NonNull String mode) {
        JSONObject modeObject = getModeObject(mode);
        return modeObject != null && modeObject.optBoolean("terminal_panel", true);
    }

    public boolean hasWorkspaceWebview(@NonNull String mode) {
        JSONObject modeObject = getModeObject(mode);
        return modeObject != null && modeObject.optBoolean("workspace_webview", false);
    }

    @NonNull
    public String getAgentPanel(@NonNull String mode) {
        JSONObject modeObject = getModeObject(mode);
        if (modeObject == null) return "control_webview";
        return modeObject.optString("agent_panel", "control_webview");
    }

    @Nullable
    public JSONObject getModeObject(@NonNull String mode) {
        JSONObject modes = mRoot.optJSONObject("modes");
        if (modes == null) return null;
        return modes.optJSONObject(mode);
    }

    @NonNull
    public String getDefaultSessionMode(@NonNull String mode) {
        JSONObject modeObject = getModeObject(mode);
        if (modeObject == null) return "isolated";
        return modeObject.optString("default_session_mode", "isolated");
    }

    @NonNull
    public String getDefaultTargetContext(@NonNull String mode) {
        JSONObject modeObject = getModeObject(mode);
        if (modeObject == null) return "headless";
        return modeObject.optString("default_target_context", "headless");
    }

    @NonNull
    public List<String> getDefaultResultDelivery(@NonNull String mode) {
        JSONObject modeObject = getModeObject(mode);
        if (modeObject == null) return Collections.emptyList();
        return jsonArrayToList(modeObject.optJSONArray("default_result_delivery"));
    }

    @Nullable
    public String getConventionalSessionProfile(@NonNull String mode, @NonNull String sessionMode) {
        String profileName = mode + "_" + sessionMode;
        JSONObject sessionProfiles = mRoot.optJSONObject("session_profiles");
        if (sessionProfiles == null) return null;
        return sessionProfiles.has(profileName) ? profileName : null;
    }

    @Nullable
    public JSONObject getTaskDefault(@NonNull String taskName) {
        JSONObject taskDefaults = mRoot.optJSONObject("task_defaults");
        if (taskDefaults == null) return null;
        return taskDefaults.optJSONObject(taskName);
    }

    @Nullable
    public JSONObject getSessionProfile(@NonNull String profileName) {
        JSONObject sessionProfiles = mRoot.optJSONObject("session_profiles");
        if (sessionProfiles == null) return null;
        return sessionProfiles.optJSONObject(profileName);
    }

    @NonNull
    public JSONObject toJson() {
        return mRoot;
    }

    @NonNull
    private static List<String> jsonArrayToList(@Nullable JSONArray jsonArray) {
        if (jsonArray == null || jsonArray.length() == 0) return Collections.emptyList();

        ArrayList<String> values = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            values.add(jsonArray.optString(i));
        }

        return values;
    }

}
