package com.termux.app.browser;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.TermuxActivity;
import com.termux.app.config.ModeConfig;
import com.termux.app.config.ModeConfigManager;
import com.termux.shared.notification.NotificationUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.notification.TermuxNotificationUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class TermuxBrowserBridge {

    private static final TermuxBrowserBridge INSTANCE = new TermuxBrowserBridge();

    private final Object mControllerLock = new Object();
    private final Object mCommandLock = new Object();
    private final Map<String, TermuxBrowserController> mControllers = new HashMap<>();
    @Nullable private final Map<String, TermuxHeadlessBrowserController> mHeadlessControllers = new HashMap<>();

    private TermuxBrowserBridge() {}

    @NonNull
    public static TermuxBrowserBridge getInstance() {
        return INSTANCE;
    }

    public void attachController(@NonNull String mode, @NonNull String targetContext,
                                 @NonNull TermuxBrowserController controller) {
        synchronized (mControllerLock) {
            mControllers.put(getControllerKey(mode, targetContext), controller);
            mControllerLock.notifyAll();
        }
    }

    public void detachController(@NonNull String mode, @NonNull String targetContext,
                                 @NonNull TermuxBrowserController controller) {
        synchronized (mControllerLock) {
            String controllerKey = getControllerKey(mode, targetContext);
            TermuxBrowserController current = mControllers.get(controllerKey);
            if (current == controller) {
                mControllers.remove(controllerKey);
            }
        }
    }

    @NonNull
    public BrowserCommandResult execute(@NonNull Context context, @NonNull BrowserCommandRequest request) {
        synchronized (mCommandLock) {
            ModeConfig modeConfig = ModeConfigManager.getModeConfig();
            if (!modeConfig.isModeEnabled(request.mode)) {
                BrowserCommandResult result = withMeta(BrowserCommandResult.error(request.action, "MODE_DISABLED",
                    "Mode is disabled by configuration: " + request.mode),
                    request, request.targetContext,
                    new BrowserSessionDecision(request.sessionMode, request.sessionMode,
                        request.sessionProfile, request.sessionProfile, request.shareFrom, request.shareFrom));
                deliverResult(context, modeConfig, request, request.targetContext, result);
                return result;
            }

            String resolvedTargetContext = resolveTargetContext(modeConfig, request);
            BrowserSessionDecision sessionDecision = resolveSessionDecision(modeConfig, request, resolvedTargetContext);

            if ("unsupported".equals(sessionDecision.resolvedSessionMode)) {
                BrowserCommandResult result = withMeta(BrowserCommandResult.error(request.action, "ISOLATED_SESSION_NOT_AVAILABLE",
                    "Isolated session mode is not available for the visible browser runtime in this build."),
                    request, resolvedTargetContext, sessionDecision);
                deliverResult(context, modeConfig, request, resolvedTargetContext, result);
                return result;
            }

            if ("headless".equals(resolvedTargetContext)) {
                BrowserCommandResult result = withMeta(getOrCreateHeadlessController(context, sessionDecision).execute(request),
                    request, resolvedTargetContext, sessionDecision);
                deliverResult(context, modeConfig, request, resolvedTargetContext, result);
                return result;
            }

            TermuxBrowserWorkspace.launchTermuxActivity(context, request.mode);
            TermuxBrowserController controller = awaitController(request.mode, resolvedTargetContext, request.timeoutMs);
            if (controller == null) {
                BrowserCommandResult result = withMeta(BrowserCommandResult.error(request.action, "BROWSER_NOT_READY",
                    "Browser runtime is not attached to the foreground activity."),
                    request, resolvedTargetContext, sessionDecision);
                deliverResult(context, modeConfig, request, resolvedTargetContext, result);
                return result;
            }

            BrowserCommandResult result = withMeta(controller.execute(request), request, resolvedTargetContext, sessionDecision);
            deliverResult(context, modeConfig, request, resolvedTargetContext, result);
            return result;
        }
    }

    @NonNull
    private TermuxHeadlessBrowserController getOrCreateHeadlessController(@NonNull Context context,
                                                                          @NonNull BrowserSessionDecision sessionDecision) {
        String runtimeKey = getHeadlessRuntimeKey(sessionDecision);
        synchronized (mControllerLock) {
            TermuxHeadlessBrowserController controller = mHeadlessControllers.get(runtimeKey);
            if (controller != null) return controller;

            controller = TermuxHeadlessBrowserController.create(context);
            mHeadlessControllers.put(runtimeKey, controller);
            return controller;
        }
    }

    @NonNull
    private String getHeadlessRuntimeKey(@NonNull BrowserSessionDecision sessionDecision) {
        if (sessionDecision.resolvedSessionProfile != null) {
            return "profile:" + sessionDecision.resolvedSessionProfile;
        }

        if ("isolated".equals(sessionDecision.resolvedSessionMode)) {
            return "isolated-default";
        }

        return "shared-default";
    }

    @NonNull
    private String resolveTargetContext(@NonNull ModeConfig modeConfig, @NonNull BrowserCommandRequest request) {
        if ("headless".equals(request.targetContext)) {
            if ("automation".equals(request.mode)) {
                return "headless";
            }

            if (request.targetContextExplicit) {
                return "headless";
            }

            return "workspace_webview";
        }

        if ("none".equals(modeConfig.getLayout(request.mode))) {
            return "headless";
        }

        if ("visible".equals(modeConfig.getBrowserRuntime(request.mode))) {
            return request.targetContext;
        }

        return "headless";
    }

    @NonNull
    private BrowserSessionDecision resolveSessionDecision(@NonNull ModeConfig modeConfig,
                                                          @NonNull BrowserCommandRequest request,
                                                          @NonNull String resolvedTargetContext) {
        String resolvedSessionMode = request.sessionMode;
        if ("isolated".equals(request.sessionMode) && !"headless".equals(resolvedTargetContext)) {
            resolvedSessionMode = "unsupported";
        }

        String resolvedSessionProfile = request.sessionProfile;
        if (resolvedSessionProfile == null) {
            resolvedSessionProfile = modeConfig.getConventionalSessionProfile(request.mode, request.sessionMode);
        }

        String resolvedShareFrom = request.shareFrom;
        if (resolvedShareFrom == null && "shared".equals(resolvedSessionMode) && !"headless".equals(resolvedTargetContext)) {
            resolvedShareFrom = resolvedTargetContext;
        }

        return new BrowserSessionDecision(request.sessionMode, resolvedSessionMode,
            request.sessionProfile, resolvedSessionProfile, request.shareFrom, resolvedShareFrom);
    }

    @NonNull
    private BrowserCommandResult withMeta(@NonNull BrowserCommandResult result,
                                          @NonNull BrowserCommandRequest request,
                                          @NonNull String resolvedTargetContext,
                                          @NonNull BrowserSessionDecision sessionDecision) {
        JSONObject meta = new JSONObject();
        try {
            meta.put("mode", request.mode);
            if (request.taskDefault != null) meta.put("task_default", request.taskDefault);
            meta.put("session_mode", request.sessionMode);
            meta.put("resolved_session_mode", sessionDecision.resolvedSessionMode);
            if (request.sessionProfile != null) meta.put("session_profile", request.sessionProfile);
            if (sessionDecision.resolvedSessionProfile != null) meta.put("resolved_session_profile", sessionDecision.resolvedSessionProfile);
            if (request.shareFrom != null) meta.put("share_from", request.shareFrom);
            if (sessionDecision.resolvedShareFrom != null) meta.put("resolved_share_from", sessionDecision.resolvedShareFrom);
            meta.put("target_context", request.targetContext);
            meta.put("resolved_target_context", resolvedTargetContext);
            meta.put("result_delivery", request.resultDelivery);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }

        return result.withMeta(meta);
    }

    private void deliverResult(@NonNull Context context,
                               @NonNull ModeConfig modeConfig,
                               @NonNull BrowserCommandRequest request,
                               @NonNull String resolvedTargetContext,
                               @NonNull BrowserCommandResult result) {
        List<String> targets = parseResultDeliveryTargets(request.resultDelivery);
        if (targets.isEmpty()) return;

        String summary = buildResultSummary(request, result);
        for (String target : targets) {
            switch (target) {
                case "control_webview":
                case "workspace_webview":
                    deliverToVisibleTarget(context, modeConfig, request, target, summary);
                    break;
                case "notification":
                    deliverNotification(context, summary, request, resolvedTargetContext, result);
                    break;
                case "terminal":
                default:
                    break;
            }
        }
    }

    private void deliverToVisibleTarget(@NonNull Context context,
                                        @NonNull ModeConfig modeConfig,
                                        @NonNull BrowserCommandRequest request,
                                        @NonNull String targetContext,
                                        @NonNull String summary) {
        TermuxBrowserController controller;
        synchronized (mControllerLock) {
            controller = mControllers.get(getControllerKey(request.mode, targetContext));
        }

        if (controller == null &&
            "visible".equals(modeConfig.getBrowserRuntime(request.mode)) &&
            !"none".equals(modeConfig.getLayout(request.mode))) {
            TermuxBrowserWorkspace.launchTermuxActivity(context, request.mode);
            controller = awaitController(request.mode, targetContext, Math.min(request.timeoutMs, 2000L));
        }

        if (controller != null) {
            controller.showCommandDelivery(summary);
        }
    }

    private void deliverNotification(@NonNull Context context,
                                     @NonNull String summary,
                                     @NonNull BrowserCommandRequest request,
                                     @NonNull String resolvedTargetContext,
                                     @NonNull BrowserCommandResult result) {
        NotificationUtils.setupNotificationChannel(context,
            TermuxConstants.TERMUX_APP_NOTIFICATION_CHANNEL_ID,
            TermuxConstants.TERMUX_APP_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW);

        Intent activityIntent = new Intent(context, TermuxActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activityIntent.putExtra(TermuxBrowserWorkspace.EXTRA_TARGET_MODE, request.mode);

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(context,
            request.requestId.hashCode(), activityIntent, pendingIntentFlags);

        CharSequence title = result.isOk() ?
            "Browser command completed" :
            "Browser command failed";
        Notification.Builder builder = TermuxNotificationUtils.getTermuxOrPluginAppNotificationBuilder(
            context, context, TermuxConstants.TERMUX_APP_NOTIFICATION_CHANNEL_ID,
            Notification.PRIORITY_LOW, title, summary, summary, contentIntent, null,
            NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null) return;

        builder.setSubText("mode=" + request.mode + " target=" + resolvedTargetContext);

        NotificationManager notificationManager = NotificationUtils.getNotificationManager(context);
        if (notificationManager != null) {
            notificationManager.notify(TermuxNotificationUtils.getNextNotificationId(context), builder.build());
        }
    }

    @NonNull
    private List<String> parseResultDeliveryTargets(@Nullable String resultDelivery) {
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        if (resultDelivery == null) return new java.util.ArrayList<>();

        for (String value : resultDelivery.split(",")) {
            value = value.trim();
            if (!value.isEmpty()) {
                targets.add(value);
            }
        }

        return new java.util.ArrayList<>(targets);
    }

    @NonNull
    private String buildResultSummary(@NonNull BrowserCommandRequest request,
                                      @NonNull BrowserCommandResult result) {
        if (result.isOk()) {
            JSONObject resultObject = result.getResultObject();
            String url = resultObject != null ? resultObject.optString("url", null) : null;
            if (!TextUtils.isEmpty(url)) {
                return request.action + " succeeded: " + url;
            }
            return request.action + " succeeded";
        }

        JSONObject error = result.getErrorObject();
        String code = error != null ? error.optString("code", "ERROR") : "ERROR";
        String message = error != null ? error.optString("message", "Command failed.") : "Command failed.";
        if (message.length() > 120) {
            message = message.substring(0, 117) + "...";
        }
        return request.action + " failed: " + code + " - " + message;
    }

    @Nullable
    private TermuxBrowserController awaitController(@NonNull String mode, @NonNull String targetContext, long timeoutMs) {
        long waitUntil = System.currentTimeMillis() + Math.max(timeoutMs, TimeUnit.SECONDS.toMillis(3));
        String controllerKey = getControllerKey(mode, targetContext);
        synchronized (mControllerLock) {
            while (!mControllers.containsKey(controllerKey)) {
                long remaining = waitUntil - System.currentTimeMillis();
                if (remaining <= 0) return null;

                try {
                    mControllerLock.wait(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            return mControllers.get(controllerKey);
        }
    }

    @NonNull
    private String getControllerKey(@NonNull String mode, @NonNull String targetContext) {
        return mode + "::" + targetContext;
    }

}
