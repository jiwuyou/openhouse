package com.termux.app.browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BrowserCommandReceiver extends BroadcastReceiver {

    public static final String EXTRA_REQUEST_ID = "request_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String requestId = intent != null ? intent.getStringExtra(EXTRA_REQUEST_ID) : null;
        if (!TermuxBrowserWorkspace.isValidRequestId(requestId) ||
            !TermuxBrowserWorkspace.requestDirectoryExists(requestId)) {
            return;
        }

        final PendingResult pendingResult = goAsync();
        final Context appContext = context.getApplicationContext();

        new Thread(() -> {
            try {
                TermuxBrowserWorkspace.appendDebugLog(requestId, "receiver:start");
                TermuxBrowserWorkspace.ensureRuntimeSetup(appContext);
                TermuxBrowserWorkspace.appendDebugLog(requestId, "receiver:runtime_ready");
                BrowserCommandRequest request = BrowserCommandRequest.fromRequestId(requestId);
                TermuxBrowserWorkspace.appendDebugLog(requestId, "receiver:parsed:" + request.action);
                BrowserCommandResult result = TermuxBrowserBridge.getInstance().execute(appContext, request);
                TermuxBrowserWorkspace.appendDebugLog(requestId, "receiver:executed");
                TermuxBrowserWorkspace.writeResponse(requestId, result);
                TermuxBrowserWorkspace.appendDebugLog(requestId, "receiver:response_written");
            } catch (IllegalArgumentException e) {
                if (requestId != null && !requestId.trim().isEmpty()) {
                    TermuxBrowserWorkspace.appendDebugLog(requestId, "receiver:illegal_argument:" + e.getMessage());
                    TermuxBrowserWorkspace.writeResponseError(requestId, "unknown", "INVALID_ARGUMENT", e.getMessage());
                }
            } catch (Exception e) {
                if (requestId != null && !requestId.trim().isEmpty()) {
                    TermuxBrowserWorkspace.appendDebugLog(requestId, "receiver:exception:" + (e.getMessage() != null ? e.getMessage() : e.toString()));
                    TermuxBrowserWorkspace.writeResponseError(requestId, "unknown", "UNEXPECTED_ERROR",
                        e.getMessage() != null ? e.getMessage() : e.toString());
                }
            } finally {
                pendingResult.finish();
            }
        }, "termux-browser-command").start();
    }

}
