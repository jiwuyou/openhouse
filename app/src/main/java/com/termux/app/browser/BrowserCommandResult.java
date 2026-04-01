package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class BrowserCommandResult {

    public final int exitCode;
    @NonNull public final JSONObject payload;

    private BrowserCommandResult(int exitCode, @NonNull JSONObject payload) {
        this.exitCode = exitCode;
        this.payload = payload;
    }

    @NonNull
    public static BrowserCommandResult success(@NonNull String action, @Nullable JSONObject result) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("ok", true);
            payload.put("action", action);
            payload.put("result", result != null ? result : new JSONObject());
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }

        return new BrowserCommandResult(0, payload);
    }

    @NonNull
    public static BrowserCommandResult success(@NonNull String action, @NonNull JSONArray result) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("ok", true);
            payload.put("action", action);
            payload.put("result", result);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }

        return new BrowserCommandResult(0, payload);
    }

    @NonNull
    public static BrowserCommandResult error(@NonNull String action, @NonNull String code, @NonNull String message) {
        JSONObject payload = new JSONObject();
        JSONObject error = new JSONObject();
        try {
            error.put("code", code);
            error.put("message", message);
            payload.put("ok", false);
            payload.put("action", action);
            payload.put("error", error);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }

        return new BrowserCommandResult(1, payload);
    }

    @NonNull
    public BrowserCommandResult withMeta(@NonNull JSONObject meta) {
        try {
            payload.put("meta", meta);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }

        return this;
    }

    public boolean isOk() {
        return payload.optBoolean("ok", false);
    }

    @Nullable
    public JSONObject getResultObject() {
        return payload.optJSONObject("result");
    }

    @Nullable
    public JSONObject getErrorObject() {
        return payload.optJSONObject("error");
    }

}
