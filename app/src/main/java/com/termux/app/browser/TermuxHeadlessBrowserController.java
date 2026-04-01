package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class TermuxHeadlessBrowserController {

    private static final String LOG_TAG = "TermuxHeadlessBrowser";
    private static final int MAX_CONSOLE_ERRORS = 50;
    private static final long PAGE_SETTLE_DELAY_MS = 600;

    @NonNull private final Context mContext;
    @NonNull private final WebView mWebView;
    @NonNull private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    @NonNull private final ArrayDeque<JSONObject> mConsoleErrors = new ArrayDeque<>();

    @Nullable private PendingPageLoad mPendingPageLoad;

    private TermuxHeadlessBrowserController(@NonNull Context context, @NonNull WebView webView) {
        mContext = context;
        mWebView = webView;
        setupWebView();
    }

    @NonNull
    static TermuxHeadlessBrowserController create(@NonNull Context context) {
        AtomicReference<TermuxHeadlessBrowserController> controllerRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            try {
                controllerRef.set(new TermuxHeadlessBrowserController(context.getApplicationContext(), new WebView(context.getApplicationContext())));
            } finally {
                latch.countDown();
            }
        });

        awaitLatch(latch, 5000L);
        TermuxHeadlessBrowserController controller = controllerRef.get();
        if (controller == null) {
            throw new IllegalStateException("Failed to create headless browser controller.");
        }

        return controller;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        }

        mWebView.setWebViewClient(new RuntimeWebViewClient());
        mWebView.setWebChromeClient(new RuntimeWebChromeClient());
        mWebView.loadUrl("about:blank");
    }

    @NonNull
    BrowserCommandResult execute(@NonNull BrowserCommandRequest request) {
        switch (request.action) {
            case "open":
                return open(request);
            case "click":
                return click(request);
            case "type":
                return type(request);
            case "scroll":
                return scroll(request);
            case "read_text":
                return readText(request);
            case "read_dom":
                return readDom(request);
            case "read_console":
                return readConsole(request);
            case "screenshot":
                return BrowserCommandResult.error(request.action, "NOT_SUPPORTED",
                    "Screenshot capture is not supported for the headless browser runtime.");
            default:
                return BrowserCommandResult.error(request.action, "INVALID_ARGUMENT",
                    "Unsupported browser action: " + request.action);
        }
    }

    @NonNull
    private BrowserCommandResult open(@NonNull BrowserCommandRequest request) {
        if (TextUtils.isEmpty(request.url)) {
            return BrowserCommandResult.error(request.action, "INVALID_ARGUMENT", "open requires a url.");
        }

        String normalizedUrl = normalizeUrl(request.url);
        AtomicReference<String> finalUrl = new AtomicReference<>(normalizedUrl);
        AtomicReference<String> title = new AtomicReference<>("");
        AtomicReference<String> error = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);
        clearConsoleErrors();
        mPendingPageLoad = new PendingPageLoad(normalizedUrl, latch, finalUrl, title, error);

        runOnMainThread(() -> {
            Uri normalizedUri = Uri.parse(normalizedUrl);
            String previewText = TermuxBrowserWorkspace.readWorkspacePreviewText(normalizedUri);
            if (previewText != null) {
                mWebView.loadDataWithBaseURL(normalizedUrl, previewText, "text/html", "UTF-8", normalizedUrl);
            } else {
                mWebView.loadUrl(normalizedUrl);
            }
        });

        if (!awaitLatch(latch, request.timeoutMs)) {
            return BrowserCommandResult.error(request.action, "TIMEOUT", "Timed out waiting for page load.");
        }

        if (error.get() != null) {
            return BrowserCommandResult.error(request.action, "NAVIGATION_FAILED", error.get());
        }

        JSONObject result = new JSONObject();
        try {
            result.put("url", finalUrl.get());
            result.put("title", title.get());
        } catch (JSONException e) {
            return BrowserCommandResult.error(request.action, "SERIALIZATION_FAILED", e.getMessage());
        }

        return BrowserCommandResult.success(request.action, result);
    }

    @NonNull
    private BrowserCommandResult click(@NonNull BrowserCommandRequest request) {
        if (TextUtils.isEmpty(request.selector)) {
            return BrowserCommandResult.error(request.action, "INVALID_ARGUMENT", "click requires --selector.");
        }

        String script = "(function(){var selector=" + JSONObject.quote(request.selector) + ";" +
            "var element=document.querySelector(selector);" +
            "if(!element){return JSON.stringify({ok:false,code:'ELEMENT_NOT_FOUND',message:'No element matched selector: '+selector});}" +
            "element.focus();" +
            "['mouseover','mousedown','mouseup','click'].forEach(function(type){element.dispatchEvent(new MouseEvent(type,{bubbles:true,cancelable:true,view:window}));});" +
            "if (typeof element.click === 'function') { element.click(); }" +
            "var tag=(element.tagName||'').toUpperCase();" +
            "var inputType=(element.type||'').toLowerCase();" +
            "if (tag==='A' && element.href) { window.location.href = element.href; }" +
            "if (element.form && (tag==='BUTTON' || (tag==='INPUT' && (inputType==='submit' || inputType==='image')))) {" +
            "  if (typeof element.form.requestSubmit === 'function') { element.form.requestSubmit(element); } else { element.form.submit(); }" +
            "}" +
            "return JSON.stringify({ok:true,url:location.href});})()";

        return evaluateStructuredScript(request, script);
    }

    @NonNull
    private BrowserCommandResult type(@NonNull BrowserCommandRequest request) {
        if (TextUtils.isEmpty(request.selector) || request.text == null) {
            return BrowserCommandResult.error(request.action, "INVALID_ARGUMENT", "type requires --selector and --text.");
        }

        String script = "(function(){var selector=" + JSONObject.quote(request.selector) + ";" +
            "var value=" + JSONObject.quote(request.text) + ";" +
            "var element=document.querySelector(selector);" +
            "if(!element){return JSON.stringify({ok:false,code:'ELEMENT_NOT_FOUND',message:'No element matched selector: '+selector});}" +
            "element.focus();" +
            "element.value=value;" +
            "element.dispatchEvent(new Event('input',{bubbles:true}));" +
            "element.dispatchEvent(new Event('change',{bubbles:true}));" +
            "return JSON.stringify({ok:true,value:element.value});})()";

        return evaluateStructuredScript(request, script);
    }

    @NonNull
    private BrowserCommandResult scroll(@NonNull BrowserCommandRequest request) {
        String script = "(function(){window.scrollBy(0," + request.deltaY + ");return JSON.stringify({ok:true,scrollY:window.scrollY});})()";
        return evaluateStructuredScript(request, script);
    }

    @NonNull
    private BrowserCommandResult readText(@NonNull BrowserCommandRequest request) {
        String script = "(function(){return JSON.stringify({ok:true,url:location.href,text:(document.body&&document.body.innerText)||''});})()";
        return evaluateStructuredScript(request, script);
    }

    @NonNull
    private BrowserCommandResult readDom(@NonNull BrowserCommandRequest request) {
        String script = "(function(){return JSON.stringify({ok:true,url:location.href,dom:document.documentElement?document.documentElement.outerHTML:''});})()";
        return evaluateStructuredScript(request, script);
    }

    @NonNull
    private BrowserCommandResult readConsole(@NonNull BrowserCommandRequest request) {
        JSONArray result = new JSONArray();
        synchronized (mConsoleErrors) {
            Iterator<JSONObject> iterator = mConsoleErrors.iterator();
            while (iterator.hasNext()) {
                result.put(iterator.next());
            }
        }

        return BrowserCommandResult.success(request.action, result);
    }

    @NonNull
    private BrowserCommandResult evaluateStructuredScript(@NonNull BrowserCommandRequest request, @NonNull String script) {
        AtomicReference<String> rawResult = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        runOnMainThread(() -> mWebView.evaluateJavascript(script, value -> {
            rawResult.set(value);
            latch.countDown();
        }));

        if (!awaitLatch(latch, request.timeoutMs)) {
            return BrowserCommandResult.error(request.action, "TIMEOUT", "Timed out waiting for script execution.");
        }

        try {
            JSONObject data = parseJavascriptObject(rawResult.get());
            if (!data.optBoolean("ok", false)) {
                return BrowserCommandResult.error(request.action, data.optString("code", "SCRIPT_EXECUTION_FAILED"),
                    data.optString("message", "Browser action failed."));
            }
            return BrowserCommandResult.success(request.action, data);
        } catch (JSONException e) {
            return BrowserCommandResult.error(request.action, "SCRIPT_EXECUTION_FAILED", e.getMessage());
        }
    }

    @NonNull
    private JSONObject parseJavascriptObject(@Nullable String raw) throws JSONException {
        if (raw == null || "null".equals(raw)) {
            return new JSONObject();
        }

        Object value = new JSONTokener(raw).nextValue();
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }
        if (value instanceof String) {
            return new JSONObject((String) value);
        }

        return new JSONObject();
    }

    @NonNull
    private String normalizeUrl(@NonNull String url) {
        if (url.startsWith("file://")) {
            String localPath = Uri.parse(url).getPath();
            if (localPath != null && TermuxBrowserWorkspace.isWorkspaceFilePath(localPath)) {
                return TermuxBrowserWorkspace.toWorkspacePreviewUrl(localPath);
            }
        }
        if (url.startsWith("/")) {
            if (TermuxBrowserWorkspace.isWorkspaceFilePath(url)) {
                return TermuxBrowserWorkspace.toWorkspacePreviewUrl(url);
            }
        }
        Uri parsed = Uri.parse(url);
        if (!TextUtils.isEmpty(parsed.getScheme())) {
            return url;
        }
        return "https://" + url;
    }

    private void clearConsoleErrors() {
        synchronized (mConsoleErrors) {
            mConsoleErrors.clear();
        }
    }

    private void pushConsoleError(@NonNull ConsoleMessage consoleMessage) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("message", consoleMessage.message());
            payload.put("source", consoleMessage.sourceId());
            payload.put("line", consoleMessage.lineNumber());
            payload.put("level", consoleMessage.messageLevel().name());
            payload.put("url", mWebView.getUrl());
        } catch (JSONException e) {
            Logger.logErrorExtended(LOG_TAG, e.toString());
        }

        synchronized (mConsoleErrors) {
            if (mConsoleErrors.size() >= MAX_CONSOLE_ERRORS) {
                mConsoleErrors.removeFirst();
            }
            mConsoleErrors.addLast(payload);
        }
    }

    private void finishPendingLoad(@Nullable String finalUrl, @Nullable String errorMessage) {
        PendingPageLoad pendingPageLoad = mPendingPageLoad;
        if (pendingPageLoad == null) return;

        if (errorMessage != null) {
            if (pendingPageLoad.finished.compareAndSet(false, true)) {
                if (finalUrl != null) {
                    pendingPageLoad.url.set(finalUrl);
                }
                pendingPageLoad.error.set(errorMessage);
                pendingPageLoad.latch.countDown();
                mPendingPageLoad = null;
            }
            return;
        }

        if (finalUrl != null) {
            pendingPageLoad.url.set(finalUrl);
        }

        long signalAt = SystemClock.elapsedRealtime();
        pendingPageLoad.lastSignalAt.set(signalAt);
        mMainHandler.postDelayed(() -> {
            PendingPageLoad current = mPendingPageLoad;
            if (current != pendingPageLoad) return;
            if (current.finished.get()) return;
            if (!current.lastSignalAt.get().equals(signalAt)) return;

            if (current.finished.compareAndSet(false, true)) {
                current.latch.countDown();
                mPendingPageLoad = null;
            }
        }, PAGE_SETTLE_DELAY_MS);
    }

    private void resetPendingLoadIfNewNavigation(@Nullable String url) {
        PendingPageLoad pendingPageLoad = mPendingPageLoad;
        if (pendingPageLoad == null || pendingPageLoad.finished.get()) return;
        if (url == null || url.equals(pendingPageLoad.requestedUrl) ||
            url.startsWith(pendingPageLoad.requestedUrl) || pendingPageLoad.requestedUrl.startsWith(url)) {
            return;
        }

        pendingPageLoad.url.set(url);
        pendingPageLoad.lastSignalAt.set(SystemClock.elapsedRealtime());
    }

    private void runOnMainThread(@NonNull Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            mMainHandler.post(() -> {
                try {
                    runnable.run();
                } finally {
                    latch.countDown();
                }
            });
            awaitLatch(latch, 3000L);
        }
    }

    private static boolean awaitLatch(@NonNull CountDownLatch latch, long timeoutMs) {
        try {
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private final class RuntimeWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            resetPendingLoadIfNewNavigation(url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            finishPendingLoad(url, null);
            super.onPageFinished(view, url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (request != null && request.getUrl() != null) {
                WebResourceResponse response = TermuxBrowserWorkspace.openWorkspacePreviewResponse(request.getUrl());
                if (response != null) return response;
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        @SuppressWarnings("deprecation")
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url != null) {
                WebResourceResponse response = TermuxBrowserWorkspace.openWorkspacePreviewResponse(Uri.parse(url));
                if (response != null) return response;
            }
            return super.shouldInterceptRequest(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                finishPendingLoad(view.getUrl(),
                    error.getDescription() != null ? error.getDescription().toString() : "Navigation failed.");
            }
            super.onReceivedError(view, request, error);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            finishPendingLoad(failingUrl, description != null ? description : "Navigation failed.");
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    private final class RuntimeWebChromeClient extends WebChromeClient {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            PendingPageLoad pendingPageLoad = mPendingPageLoad;
            if (pendingPageLoad != null) {
                pendingPageLoad.title.set(title);
            }
            super.onReceivedTitle(view, title);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                pushConsoleError(consoleMessage);
            }
            return super.onConsoleMessage(consoleMessage);
        }
    }

    private static final class PendingPageLoad {
        final String requestedUrl;
        final CountDownLatch latch;
        final AtomicReference<String> url;
        final AtomicReference<String> title;
        final AtomicReference<String> error;
        final AtomicBoolean finished = new AtomicBoolean(false);
        final AtomicReference<Long> lastSignalAt = new AtomicReference<>(0L);

        PendingPageLoad(@NonNull String requestedUrl, @NonNull CountDownLatch latch,
                        @NonNull AtomicReference<String> url, @NonNull AtomicReference<String> title,
                        @NonNull AtomicReference<String> error) {
            this.requestedUrl = requestedUrl;
            this.latch = latch;
            this.url = url;
            this.title = title;
            this.error = error;
        }
    }

}
