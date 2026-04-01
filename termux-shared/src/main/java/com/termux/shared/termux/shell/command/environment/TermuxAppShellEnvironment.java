package com.termux.shared.termux.shell.command.environment;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.android.PackageUtils;
import com.termux.shared.android.SELinuxUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.settings.properties.SharedProperties;
import com.termux.shared.shell.command.environment.ShellEnvironmentUtils;
import com.termux.shared.termux.TermuxBootstrap;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.shell.am.TermuxAmSocketServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Environment for {@link TermuxConstants#TERMUX_PACKAGE_NAME} app.
 */
public class TermuxAppShellEnvironment {

    /** Termux app environment variables. */
    public static HashMap<String, String> termuxAppEnvironment;

    /** Environment variable for the Termux app version. */
    public static final String ENV_TERMUX_VERSION = TermuxConstants.TERMUX_ENV_PREFIX_ROOT + "_VERSION";

    /** Environment variable prefix for the Termux app. */
    public static final String TERMUX_APP_ENV_PREFIX = TermuxConstants.TERMUX_ENV_PREFIX_ROOT + "_APP__";

    /** Environment variable for the Termux app version name. */
    public static final String ENV_TERMUX_APP__VERSION_NAME = TERMUX_APP_ENV_PREFIX + "VERSION_NAME";
    /** Environment variable for the Termux app version code. */
    public static final String ENV_TERMUX_APP__VERSION_CODE = TERMUX_APP_ENV_PREFIX + "VERSION_CODE";
    /** Environment variable for the Termux app package name. */
    public static final String ENV_TERMUX_APP__PACKAGE_NAME = TERMUX_APP_ENV_PREFIX + "PACKAGE_NAME";
    /** Environment variable for the Termux app process id. */
    public static final String ENV_TERMUX_APP__PID = TERMUX_APP_ENV_PREFIX + "PID";
    /** Environment variable for the Termux app uid. */
    public static final String ENV_TERMUX_APP__UID = TERMUX_APP_ENV_PREFIX + "UID";
    /** Environment variable for the Termux app targetSdkVersion. */
    public static final String ENV_TERMUX_APP__TARGET_SDK = TERMUX_APP_ENV_PREFIX + "TARGET_SDK";
    /** Environment variable for the Termux app is debuggable apk build. */
    public static final String ENV_TERMUX_APP__IS_DEBUGGABLE_BUILD = TERMUX_APP_ENV_PREFIX + "IS_DEBUGGABLE_BUILD";
    /** Environment variable for the Termux app {@link TermuxConstants} APK_RELEASE_*. */
    public static final String ENV_TERMUX_APP__APK_RELEASE = TERMUX_APP_ENV_PREFIX + "APK_RELEASE";
    /** Environment variable for the Termux app install path. */
    public static final String ENV_TERMUX_APP__APK_PATH = TERMUX_APP_ENV_PREFIX + "APK_PATH";
    /** Environment variable for the Termux app is installed on external/portable storage. */
    public static final String ENV_TERMUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE = TERMUX_APP_ENV_PREFIX + "IS_INSTALLED_ON_EXTERNAL_STORAGE";

    /** Environment variable for the Termux app process selinux context. */
    public static final String ENV_TERMUX_APP__SE_PROCESS_CONTEXT = TERMUX_APP_ENV_PREFIX + "SE_PROCESS_CONTEXT";
    /** Environment variable for the Termux app data files selinux context. */
    public static final String ENV_TERMUX_APP__SE_FILE_CONTEXT = TERMUX_APP_ENV_PREFIX + "SE_FILE_CONTEXT";
    /** Environment variable for the Termux app seInfo tag found in selinux policy used to set app process and app data files selinux context. */
    public static final String ENV_TERMUX_APP__SE_INFO = TERMUX_APP_ENV_PREFIX + "SE_INFO";
    /** Environment variable for the Termux app user id. */
    public static final String ENV_TERMUX_APP__USER_ID = TERMUX_APP_ENV_PREFIX + "USER_ID";
    /** Environment variable for the Termux app profile owner. */
    public static final String ENV_TERMUX_APP__PROFILE_OWNER = TERMUX_APP_ENV_PREFIX + "PROFILE_OWNER";

    /** Environment variable for the Termux app {@link TermuxBootstrap#TERMUX_APP_PACKAGE_MANAGER}. */
    public static final String ENV_TERMUX_APP__PACKAGE_MANAGER = TERMUX_APP_ENV_PREFIX + "PACKAGE_MANAGER";
    /** Environment variable for the Termux app {@link TermuxBootstrap#TERMUX_APP_PACKAGE_VARIANT}. */
    public static final String ENV_TERMUX_APP__PACKAGE_VARIANT = TERMUX_APP_ENV_PREFIX + "PACKAGE_VARIANT";
    /** Environment variable for the Termux app files directory. */
    public static final String ENV_TERMUX_APP__FILES_DIR = TERMUX_APP_ENV_PREFIX + "FILES_DIR";


    /** Environment variable for the Termux app {@link TermuxAmSocketServer#getTermuxAppAMSocketServerEnabled(Context)}. */
    public static final String ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED = TERMUX_APP_ENV_PREFIX + "AM_SOCKET_SERVER_ENABLED";
    /** Environment variable for the Termux browser workspace root. */
    public static final String ENV_TERMUX_APP__BROWSER_WORKSPACE_DIR = TERMUX_APP_ENV_PREFIX + "BROWSER_WORKSPACE_DIR";
    /** Environment variable for the Termux browser downloads directory. */
    public static final String ENV_TERMUX_APP__BROWSER_DOWNLOADS_DIR = TERMUX_APP_ENV_PREFIX + "BROWSER_DOWNLOADS_DIR";
    /** Environment variable for the Termux browser requests directory. */
    public static final String ENV_TERMUX_APP__BROWSER_REQUESTS_DIR = TERMUX_APP_ENV_PREFIX + "BROWSER_REQUESTS_DIR";
    /** Environment variable for the Termux browser responses directory. */
    public static final String ENV_TERMUX_APP__BROWSER_RESPONSES_DIR = TERMUX_APP_ENV_PREFIX + "BROWSER_RESPONSES_DIR";
    /** Environment variable for the Termux browser cli path. */
    public static final String ENV_TERMUX_APP__BROWSER_CLI_PATH = TERMUX_APP_ENV_PREFIX + "BROWSER_CLI_PATH";
    /** Environment variable for the Termux host bridge cli path. */
    public static final String ENV_TERMUX_APP__HOST_BRIDGE_CLI_PATH = TERMUX_APP_ENV_PREFIX + "HOST_BRIDGE_CLI_PATH";
    /** Environment variable for the mode config active file path. */
    public static final String ENV_TERMUX_APP__MODE_CONFIG_PATH = TERMUX_APP_ENV_PREFIX + "MODE_CONFIG_PATH";
    /** Environment variable for the default mode. */
    public static final String ENV_TERMUX_APP__DEFAULT_MODE = TERMUX_APP_ENV_PREFIX + "DEFAULT_MODE";
    /** Environment variable for the default mode session mode. */
    public static final String ENV_TERMUX_APP__DEFAULT_MODE_SESSION = TERMUX_APP_ENV_PREFIX + "DEFAULT_MODE_SESSION";
    /** Environment variable for the default mode target context. */
    public static final String ENV_TERMUX_APP__DEFAULT_MODE_TARGET = TERMUX_APP_ENV_PREFIX + "DEFAULT_MODE_TARGET";
    /** Environment variable for the default mode result delivery targets. */
    public static final String ENV_TERMUX_APP__DEFAULT_MODE_RESULT = TERMUX_APP_ENV_PREFIX + "DEFAULT_MODE_RESULT";



    /** Get shell environment for Termux app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        setTermuxAppEnvironment(currentPackageContext);
        return termuxAppEnvironment;
    }

    /** Set Termux app environment variables in {@link #termuxAppEnvironment}. */
    public synchronized static void setTermuxAppEnvironment(@NonNull Context currentPackageContext) {
        boolean isTermuxApp = TermuxConstants.TERMUX_PACKAGE_NAME.equals(currentPackageContext.getPackageName());

        // If current package context is of termux app and its environment is already set, then no need to set again since it won't change
        // Other apps should always set environment again since termux app may be installed/updated/deleted in background
        if (termuxAppEnvironment != null && isTermuxApp)
            return;

        termuxAppEnvironment = null;

        String packageName = TermuxConstants.TERMUX_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return;
        ApplicationInfo applicationInfo = PackageUtils.getApplicationInfoForPackage(currentPackageContext, packageName);
        if (applicationInfo == null || !applicationInfo.enabled) return;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_VERSION, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__VERSION_CODE, String.valueOf(PackageUtils.getVersionCodeForPackage(packageInfo)));

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__PACKAGE_NAME, packageName);
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__PID, TermuxUtils.getTermuxAppPID(currentPackageContext));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__UID, String.valueOf(PackageUtils.getUidForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__TARGET_SDK, String.valueOf(PackageUtils.getTargetSDKForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__IS_DEBUGGABLE_BUILD, PackageUtils.isAppForPackageADebuggableBuild(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__APK_PATH, PackageUtils.getBaseAPKPathForPackage(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE, PackageUtils.isAppInstalledOnExternalStorage(applicationInfo));

        putTermuxAPKSignature(currentPackageContext, environment);

        Context termuxPackageContext = TermuxUtils.getTermuxPackageContext(currentPackageContext);
        if (termuxPackageContext != null) {
            // An app that does not have the same sharedUserId as termux app will not be able to get
            // get termux context's classloader to get BuildConfig.TERMUX_PACKAGE_VARIANT via reflection.
            // Check TermuxBootstrap.setTermuxPackageManagerAndVariantFromTermuxApp()
            if (TermuxBootstrap.TERMUX_APP_PACKAGE_MANAGER != null)
                environment.put(ENV_TERMUX_APP__PACKAGE_MANAGER, TermuxBootstrap.TERMUX_APP_PACKAGE_MANAGER.getName());
            if (TermuxBootstrap.TERMUX_APP_PACKAGE_VARIANT != null)
                environment.put(ENV_TERMUX_APP__PACKAGE_VARIANT, TermuxBootstrap.TERMUX_APP_PACKAGE_VARIANT.getName());

            // Will not be set for plugins
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED,
                TermuxAmSocketServer.getTermuxAppAMSocketServerEnabled(currentPackageContext));

            String filesDirPath = currentPackageContext.getFilesDir().getAbsolutePath();
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__FILES_DIR, filesDirPath);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__BROWSER_WORKSPACE_DIR, TermuxConstants.TERMUX_APP.TERMUX_BROWSER_WORKSPACE_DIR_PATH);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__BROWSER_DOWNLOADS_DIR, TermuxConstants.TERMUX_APP.TERMUX_BROWSER_DOWNLOADS_DIR_PATH);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__BROWSER_REQUESTS_DIR, TermuxConstants.TERMUX_APP.TERMUX_BROWSER_REQUESTS_DIR_PATH);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__BROWSER_RESPONSES_DIR, TermuxConstants.TERMUX_APP.TERMUX_BROWSER_RESPONSES_DIR_PATH);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__BROWSER_CLI_PATH, TermuxConstants.TERMUX_APP.TERMUX_BROWSER_CLI_PATH);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__HOST_BRIDGE_CLI_PATH, TermuxConstants.TERMUX_APP.TERMUX_HOST_BRIDGE_CLI_PATH);
            putModeConfigEnvironment(environment);

            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__SE_PROCESS_CONTEXT, SELinuxUtils.getContext());
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__SE_FILE_CONTEXT, SELinuxUtils.getFileContext(filesDirPath));

            String seInfoUser = PackageUtils.getApplicationInfoSeInfoUserForPackage(applicationInfo);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__SE_INFO, PackageUtils.getApplicationInfoSeInfoForPackage(applicationInfo) +
                (DataUtils.isNullOrEmpty(seInfoUser) ? "" : seInfoUser));

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__USER_ID, String.valueOf(PackageUtils.getUserIdForPackage(currentPackageContext)));
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__PROFILE_OWNER, PackageUtils.getProfileOwnerPackageNameForUser(currentPackageContext));
        }

        termuxAppEnvironment = environment;
    }

    /** Put {@link #ENV_TERMUX_APP__APK_RELEASE} in {@code environment}. */
    public static void putTermuxAPKSignature(@NonNull Context currentPackageContext,
                                             @NonNull HashMap<String, String> environment) {
        String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(currentPackageContext,
            TermuxConstants.TERMUX_PACKAGE_NAME);
        if (signingCertificateSHA256Digest != null) {
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__APK_RELEASE,
                TermuxUtils.getAPKRelease(signingCertificateSHA256Digest).replaceAll("[^a-zA-Z]", "_").toUpperCase());
        }
    }

    /** Update {@link #ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED} value in {@code environment}. */
    public synchronized static void updateTermuxAppAMSocketServerEnabled(@NonNull Context currentPackageContext) {
        if (termuxAppEnvironment == null) return;
        termuxAppEnvironment.remove(ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED);
        ShellEnvironmentUtils.putToEnvIfSet(termuxAppEnvironment, ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED,
            TermuxAmSocketServer.getTermuxAppAMSocketServerEnabled(currentPackageContext));
    }

    private static void putModeConfigEnvironment(@NonNull HashMap<String, String> environment) {
        ModeConfigEnvironmentInfo modeConfigEnvironmentInfo = loadModeConfigEnvironmentInfo();
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__MODE_CONFIG_PATH, modeConfigEnvironmentInfo.sourcePath);
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__DEFAULT_MODE, modeConfigEnvironmentInfo.defaultMode);
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__DEFAULT_MODE_SESSION, modeConfigEnvironmentInfo.defaultSessionMode);
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__DEFAULT_MODE_TARGET, modeConfigEnvironmentInfo.defaultTargetContext);
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__DEFAULT_MODE_RESULT, modeConfigEnvironmentInfo.defaultResultDelivery);
    }

    @NonNull
    private static ModeConfigEnvironmentInfo loadModeConfigEnvironmentInfo() {
        File file = SharedProperties.getPropertiesFileFromList(TermuxConstants.TERMUX_MODE_CONFIG_FILE_PATHS_LIST,
            TermuxAppShellEnvironment.class.getSimpleName());
        String sourcePath = file != null ? file.getAbsolutePath() : "(default)";

        JSONObject root = loadModeConfigJson(file);
        if (root == null) root = buildDefaultModeConfigJson();

        String defaultMode = root.optString("default_mode", "daily");
        JSONObject modeObject = getModeObject(root, defaultMode);
        if (modeObject == null) {
            defaultMode = "daily";
            modeObject = getModeObject(buildDefaultModeConfigJson(), defaultMode);
        }

        return new ModeConfigEnvironmentInfo(
            sourcePath,
            defaultMode,
            modeObject != null ? modeObject.optString("default_session_mode", "isolated") : "isolated",
            modeObject != null ? modeObject.optString("default_target_context", "control_webview") : "control_webview",
            modeObject != null ? jsonArrayToCsv(modeObject.optJSONArray("default_result_delivery")) : ""
        );
    }

    @Nullable
    private static JSONObject loadModeConfigJson(@Nullable File file) {
        if (file == null) return null;

        StringBuilder data = new StringBuilder();
        Error error = FileUtils.readTextFromFile("mode config", file.getAbsolutePath(), StandardCharsets.UTF_8, data, false);
        if (error != null) return null;

        try {
            return new JSONObject(new JSONTokener(data.toString()));
        } catch (JSONException e) {
            return null;
        }
    }

    @NonNull
    private static JSONObject buildDefaultModeConfigJson() {
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

    @Nullable
    private static JSONObject getModeObject(@NonNull JSONObject root, @NonNull String mode) {
        JSONObject modes = root.optJSONObject("modes");
        if (modes == null) return null;
        return modes.optJSONObject(mode);
    }

    @NonNull
    private static String jsonArrayToCsv(@Nullable JSONArray jsonArray) {
        if (jsonArray == null || jsonArray.length() == 0) return "";

        StringBuilder values = new StringBuilder();
        for (int i = 0; i < jsonArray.length(); i++) {
            if (values.length() > 0) values.append(',');
            values.append(jsonArray.optString(i));
        }

        return values.toString();
    }

    private static final class ModeConfigEnvironmentInfo {
        @NonNull final String sourcePath;
        @NonNull final String defaultMode;
        @NonNull final String defaultSessionMode;
        @NonNull final String defaultTargetContext;
        @NonNull final String defaultResultDelivery;

        ModeConfigEnvironmentInfo(@NonNull String sourcePath,
                                  @NonNull String defaultMode,
                                  @NonNull String defaultSessionMode,
                                  @NonNull String defaultTargetContext,
                                  @NonNull String defaultResultDelivery) {
            this.sourcePath = sourcePath;
            this.defaultMode = defaultMode;
            this.defaultSessionMode = defaultSessionMode;
            this.defaultTargetContext = defaultTargetContext;
            this.defaultResultDelivery = defaultResultDelivery;
        }
    }

}
