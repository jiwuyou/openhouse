package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.termux.BuildConfig;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.file.TermuxFileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.Unit;
import mozilla.components.browser.toolbar.BrowserToolbar;
import mozilla.components.browser.menu.BrowserMenuBuilder;
import mozilla.components.browser.menu.BrowserMenuItem;
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem;
import mozilla.components.concept.toolbar.Toolbar;

public final class TermuxBrowserController {

    private static final String LOG_TAG = "TermuxBrowserController";
    private static final int FILE_PICKER_REQUEST_CODE = 0xB801;
    private static final int MAX_CONSOLE_ERRORS = 50;
    private static final long PAGE_SETTLE_DELAY_MS = 600;
    private static final String STATE_KEY_TAB_COUNT = "tab_count";
    private static final String STATE_KEY_CURRENT_TAB = "current_tab";
    private static final String STATE_KEY_NEXT_TAB_ID = "next_tab_id";
    private static final String STATE_KEY_URL_PREFIX = "tab_url_";
    private static final String STATE_KEY_TITLE_PREFIX = "tab_title_";
    private static final String STATE_KEY_STATE_PREFIX = "tab_state_";
    private static final String PERSIST_PREFS_NAME = "termux_browser_tabs";
    private static final String PERSIST_TABS = "tabs";
    private static final String PERSIST_CURRENT = "current";
    private static final String PERSIST_NEXT_ID = "next_id";
    private static final String PERSIST_HISTORY = "history";
    private static final String PERSIST_BOOKMARKS = "bookmarks";
    private static final int MAX_HISTORY_ITEMS = 50;

    private final Activity mActivity;
    @NonNull private final String mMode;
    @NonNull private final String mTargetContext;
    private final boolean mInteractiveChrome;
    private final boolean mCompactChrome;
    private final boolean mMinimalChrome;
    private final WebView mWebView;
    private final View mHeaderRow;
    private final TextView mStatusView;
    private final TextView mHeaderPageTitleView;
    @Nullable private final BrowserToolbar mBrowserToolbar;
    @Nullable private final View mQuickActionsRow;
    @Nullable private final HorizontalScrollView mTabStripScroll;
    @Nullable private final LinearLayout mTabStrip;
    @Nullable private final View mTabsOverlay;
    @Nullable private final LinearLayout mTabsList;
    @Nullable private final View mTabsNewButton;
    @Nullable private final View mTabsDoneButton;
    @Nullable private final View mTabsHistoryButton;
    @Nullable private final View mTabsBookmarksButton;
    @Nullable private final TextView mTabsToggleBookmarkButton;
    @Nullable private final TextView mQuickNewTabButton;
    @Nullable private final TextView mQuickTabsButton;
    @Nullable private final TextView mQuickHistoryButton;
    @Nullable private final TextView mQuickBookmarkButton;
    @Nullable private final View mLibraryOverlay;
    @Nullable private final TextView mLibraryTitleView;
    @Nullable private final LinearLayout mLibraryList;
    @Nullable private final View mLibraryDoneButton;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<JSONObject> mConsoleErrors = new ArrayDeque<>();
    @Nullable private BrowserToolbar.TwoStateButton mBackAction;
    @Nullable private BrowserToolbar.TwoStateButton mForwardAction;
    @Nullable private Toolbar.ActionButton mReloadAction;
    @Nullable private Toolbar.ActionButton mDailyMenuAction;
    @Nullable private Toolbar.Action mTabsAction;
    private final ArrayList<BrowserTab> mTabs = new ArrayList<>();
    private final ArrayList<BrowserEntry> mHistoryEntries = new ArrayList<>();
    private final ArrayList<BrowserEntry> mBookmarkEntries = new ArrayList<>();
    private int mCurrentTabIndex = -1;
    private long mNextTabId = 1L;
    @Nullable private LibraryMode mLibraryMode;

    @Nullable private ValueCallback<Uri[]> mFileChooserCallback;
    @Nullable private CountDownLatch mPendingFileChooserLatch;
    @Nullable private PendingPageLoad mPendingPageLoad;

    public TermuxBrowserController(@NonNull Activity activity, @NonNull String mode,
                                   @NonNull String targetContext,
                                   @NonNull View panelRoot, boolean interactiveChrome, boolean compactChrome,
                                   boolean minimalChrome) {
        mActivity = activity;
        mMode = mode;
        mTargetContext = targetContext;
        mInteractiveChrome = interactiveChrome;
        mCompactChrome = compactChrome;
        mMinimalChrome = minimalChrome;
        mWebView = panelRoot.findViewById(com.termux.R.id.browser_web_view);
        mStatusView = panelRoot.findViewById(com.termux.R.id.browser_status_view);
        mHeaderPageTitleView = panelRoot.findViewById(com.termux.R.id.browser_page_title_view);
        mHeaderRow = (View) mHeaderPageTitleView.getParent();
        mBrowserToolbar = panelRoot.findViewById(com.termux.R.id.browser_toolbar);
        mQuickActionsRow = panelRoot.findViewById(com.termux.R.id.browser_quick_actions_row);
        mTabStripScroll = panelRoot.findViewById(com.termux.R.id.browser_tab_strip_scroll);
        mTabStrip = panelRoot.findViewById(com.termux.R.id.browser_tab_strip);
        mQuickNewTabButton = panelRoot.findViewById(com.termux.R.id.browser_quick_new_tab_button);
        mQuickTabsButton = panelRoot.findViewById(com.termux.R.id.browser_quick_tabs_button);
        mQuickHistoryButton = panelRoot.findViewById(com.termux.R.id.browser_quick_history_button);
        mQuickBookmarkButton = panelRoot.findViewById(com.termux.R.id.browser_quick_bookmark_button);
        mTabsOverlay = panelRoot.findViewById(com.termux.R.id.browser_tabs_overlay);
        mTabsList = panelRoot.findViewById(com.termux.R.id.browser_tabs_list);
        mTabsNewButton = panelRoot.findViewById(com.termux.R.id.browser_tabs_new_button);
        mTabsDoneButton = panelRoot.findViewById(com.termux.R.id.browser_tabs_done_button);
        mTabsHistoryButton = panelRoot.findViewById(com.termux.R.id.browser_tabs_history_button);
        mTabsBookmarksButton = panelRoot.findViewById(com.termux.R.id.browser_tabs_bookmarks_button);
        mTabsToggleBookmarkButton = panelRoot.findViewById(com.termux.R.id.browser_tabs_toggle_bookmark_button);
        mLibraryOverlay = panelRoot.findViewById(com.termux.R.id.browser_library_overlay);
        mLibraryTitleView = panelRoot.findViewById(com.termux.R.id.browser_library_title);
        mLibraryList = panelRoot.findViewById(com.termux.R.id.browser_library_list);
        mLibraryDoneButton = panelRoot.findViewById(com.termux.R.id.browser_library_done_button);

        setupChrome();
        setupWebView();
        TermuxBrowserBridge.getInstance().attachController(mMode, mTargetContext, this);
        restorePersistentState();
        renderReadyState();
    }

    public void onResume() {
        mWebView.onResume();
    }

    public void onPause() {
        captureCurrentTabState();
        persistTabsState();
        mWebView.onPause();
    }

    public void saveInstanceState(@NonNull Bundle outState) {
        captureCurrentTabState();
        outState.putInt(getStateKey(STATE_KEY_TAB_COUNT), mTabs.size());
        outState.putInt(getStateKey(STATE_KEY_CURRENT_TAB), mCurrentTabIndex);
        outState.putLong(getStateKey(STATE_KEY_NEXT_TAB_ID), mNextTabId);

        for (int i = 0; i < mTabs.size(); i++) {
            BrowserTab tab = mTabs.get(i);
            outState.putString(getStateKey(STATE_KEY_URL_PREFIX + i), tab.url);
            outState.putString(getStateKey(STATE_KEY_TITLE_PREFIX + i), tab.title);
            if (tab.savedState != null) {
                outState.putBundle(getStateKey(STATE_KEY_STATE_PREFIX + i), new Bundle(tab.savedState));
            }
        }
    }

    public void restoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        int tabCount = savedInstanceState.getInt(getStateKey(STATE_KEY_TAB_COUNT), 0);
        if (tabCount <= 0) return;

        mTabs.clear();
        long restoredNextTabId = savedInstanceState.getLong(getStateKey(STATE_KEY_NEXT_TAB_ID), 1L);
        mNextTabId = Math.max(restoredNextTabId, 1L);

        for (int i = 0; i < tabCount; i++) {
            String url = savedInstanceState.getString(getStateKey(STATE_KEY_URL_PREFIX + i), getDefaultBrowserUrl());
            String title = savedInstanceState.getString(
                getStateKey(STATE_KEY_TITLE_PREFIX + i),
                mActivity.getString(R.string.browser_tab_title_default)
            );
            BrowserTab tab = new BrowserTab(mNextTabId++, getTabUrlOrDefault(url), title);
            Bundle tabState = savedInstanceState.getBundle(getStateKey(STATE_KEY_STATE_PREFIX + i));
            tab.savedState = tabState == null ? null : new Bundle(tabState);
            mTabs.add(tab);
        }

        mCurrentTabIndex = Math.min(
            Math.max(0, savedInstanceState.getInt(getStateKey(STATE_KEY_CURRENT_TAB), 0)),
            mTabs.size() - 1
        );

        BrowserTab currentTab = getCurrentTab();
        if (currentTab == null) return;

        Bundle currentState = currentTab.savedState == null ? null : new Bundle(currentTab.savedState);
        if (currentState != null) {
            mWebView.restoreState(currentState);
            updateDisplayedUrl(currentTab.url);
            updateDisplayedTitle(currentTab.title);
            updateStatus("Restored tab");
        } else {
            loadNormalizedUrl(currentTab.url, "Opening " + currentTab.url);
        }

        updateNavigationState();
    }

    public void onDestroy() {
        TermuxBrowserBridge.getInstance().detachController(mMode, mTargetContext, this);
        if (mFileChooserCallback != null) {
            mFileChooserCallback.onReceiveValue(null);
            mFileChooserCallback = null;
        }
        mWebView.destroy();
    }

    public void showCommandDelivery(@NonNull String status) {
        runOnMainThread(() -> updateStatus(status));
    }

    public boolean handleBackPressed() {
        if (isTabsOverlayVisible()) {
            hideTabsOverlay();
            return true;
        }

        if (isLibraryOverlayVisible()) {
            hideLibraryOverlay();
            return true;
        }

        if (mInteractiveChrome && mBrowserToolbar != null && mBrowserToolbar.onBackPressed()) {
            return true;
        }

        if (mWebView.canGoBack()) {
            goBack();
            return true;
        }

        return false;
    }

    public boolean canGoBack() {
        return mWebView.canGoBack();
    }

    public void goBack() {
        runOnMainThread(() -> {
            if (!mWebView.canGoBack()) return;
            mWebView.goBack();
            updateStatus("Navigating back");
            updateNavigationState();
        });
    }

    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != FILE_PICKER_REQUEST_CODE) return false;
        if (mFileChooserCallback == null) return true;

        Uri[] result = null;
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int itemCount = data.getClipData().getItemCount();
                result = new Uri[itemCount];
                for (int i = 0; i < itemCount; i++) {
                    result[i] = data.getClipData().getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                result = new Uri[] { data.getData() };
            }
        }

        mFileChooserCallback.onReceiveValue(result);
        mFileChooserCallback = null;
        return true;
    }

    @NonNull
    public BrowserCommandResult execute(@NonNull BrowserCommandRequest request) {
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
                return screenshot(request);
            default:
                return BrowserCommandResult.error(request.action, "INVALID_ARGUMENT",
                    "Unsupported browser action: " + request.action);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        }

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mWebView.setWebViewClient(new RuntimeWebViewClient());
        mWebView.setWebChromeClient(new RuntimeWebChromeClient());
        mWebView.setDownloadListener(new BrowserDownloadListener());
        mWebView.loadUrl("about:blank");
    }

    private void setupChrome() {
        if (mBrowserToolbar != null) {
            mBrowserToolbar.setVisibility(mInteractiveChrome ? View.VISIBLE : View.GONE);
        }
        if (mQuickActionsRow != null) {
            mQuickActionsRow.setVisibility(mInteractiveChrome && !mMinimalChrome ? View.VISIBLE : View.GONE);
        }
        if (mTabStripScroll != null) {
            mTabStripScroll.setVisibility(mInteractiveChrome && !mCompactChrome && !mMinimalChrome ? View.VISIBLE : View.GONE);
        }
        mHeaderPageTitleView.setVisibility(mInteractiveChrome ? View.GONE : View.VISIBLE);
        if (mTabsOverlay != null) {
            mTabsOverlay.setVisibility(View.GONE);
        }
        if (mLibraryOverlay != null) {
            mLibraryOverlay.setVisibility(View.GONE);
        }

        configurePanelDensity();

        if (!mInteractiveChrome || mBrowserToolbar == null) return;

        mBrowserToolbar.getDisplay().setHint(mActivity.getString(R.string.browser_url_hint));
        mBrowserToolbar.getDisplay().setIndicators(Collections.emptyList());

        mBackAction = new BrowserToolbar.TwoStateButton(
            ContextCompat.getDrawable(mActivity, R.drawable.browser_nav_back),
            mActivity.getString(R.string.browser_action_back_desc),
            ContextCompat.getDrawable(mActivity, R.drawable.browser_nav_back),
            mActivity.getString(R.string.browser_action_back_desc),
            () -> mWebView.canGoBack(),
            View.NO_ID,
            View.NO_ID,
            true,
            () -> 0,
            0,
            null,
            () -> {
                goBack();
                return Unit.INSTANCE;
            }
        );
        mBrowserToolbar.addNavigationAction(mBackAction);

        mForwardAction = new BrowserToolbar.TwoStateButton(
            ContextCompat.getDrawable(mActivity, R.drawable.browser_nav_forward),
            mActivity.getString(R.string.browser_action_forward_desc),
            ContextCompat.getDrawable(mActivity, R.drawable.browser_nav_forward),
            mActivity.getString(R.string.browser_action_forward_desc),
            () -> mWebView.canGoForward(),
            View.NO_ID,
            View.NO_ID,
            true,
            () -> 0,
            0,
            null,
            () -> {
                if (mWebView.canGoForward()) {
                    mWebView.goForward();
                    updateStatus("Navigating forward");
                    updateNavigationState();
                }
                return Unit.INSTANCE;
            }
        );
        mBrowserToolbar.addNavigationAction(mForwardAction);

        mTabsAction = new TabsAction();
        mBrowserToolbar.addBrowserAction(mTabsAction);

        mReloadAction = new Toolbar.ActionButton(
            ContextCompat.getDrawable(mActivity, R.drawable.browser_nav_refresh),
            mActivity.getString(R.string.browser_action_reload_desc),
            () -> true,
            () -> false,
            () -> 0,
            0,
            null,
            View.NO_ID,
            null,
            () -> {
                mWebView.reload();
                updateStatus("Reloading");
                return Unit.INSTANCE;
            }
        );
        mBrowserToolbar.addBrowserAction(mReloadAction);

        if (mMinimalChrome) {
            mDailyMenuAction = new Toolbar.ActionButton(
                ContextCompat.getDrawable(mActivity, R.drawable.browser_nav_more),
                mActivity.getString(R.string.browser_action_more_desc),
                () -> true,
                () -> false,
                () -> 0,
                0,
                null,
                View.NO_ID,
                null,
                () -> {
                    showDailyModeMenu();
                    return Unit.INSTANCE;
                }
            );
            mBrowserToolbar.addBrowserAction(mDailyMenuAction);
        }

        refreshToolbarMenu();

        mBrowserToolbar.setOnUrlCommitListener(url -> {
            String normalizedUrl = normalizeUrl(url);
            runOnMainThread(() -> loadNormalizedUrl(normalizedUrl, "Opening " + normalizedUrl));
            return true;
        });

        if (mTabsNewButton != null) {
            mTabsNewButton.setOnClickListener(v -> createNewTab(true));
        }
        if (mQuickNewTabButton != null) {
            mQuickNewTabButton.setOnClickListener(v -> createNewTab(true));
        }
        if (mTabsDoneButton != null) {
            mTabsDoneButton.setOnClickListener(v -> hideTabsOverlay());
        }
        if (mTabsHistoryButton != null) {
            mTabsHistoryButton.setOnClickListener(v -> showLibraryOverlay(LibraryMode.HISTORY));
        }
        if (mQuickHistoryButton != null) {
            mQuickHistoryButton.setOnClickListener(v -> showLibraryOverlay(LibraryMode.HISTORY));
        }
        if (mTabsBookmarksButton != null) {
            mTabsBookmarksButton.setOnClickListener(v -> showLibraryOverlay(LibraryMode.BOOKMARKS));
        }
        if (mQuickTabsButton != null) {
            mQuickTabsButton.setOnClickListener(v -> showTabsOverlay());
        }
        if (mTabsToggleBookmarkButton != null) {
            mTabsToggleBookmarkButton.setOnClickListener(v -> toggleBookmark());
        }
        if (mQuickBookmarkButton != null) {
            mQuickBookmarkButton.setOnClickListener(v -> toggleBookmark());
        }
        if (mLibraryDoneButton != null) {
            mLibraryDoneButton.setOnClickListener(v -> hideLibraryOverlay());
        }
    }

    private void configurePanelDensity() {
        if (!mInteractiveChrome) {
            mHeaderRow.setPadding(dp(10), dp(4), dp(10), dp(3));
            mHeaderPageTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
            mStatusView.setVisibility(View.GONE);
            return;
        }

        if (mMinimalChrome) {
            mHeaderRow.setVisibility(View.GONE);
            applyFixedHeight(mBrowserToolbar, 50);
            mStatusView.setVisibility(View.GONE);
            return;
        }

        if (!mCompactChrome) return;

        mHeaderRow.setVisibility(View.GONE);
        applyFixedHeight(mBrowserToolbar, 44);

        if (mQuickActionsRow != null) {
            mQuickActionsRow.setPadding(dp(6), dp(0), dp(6), dp(2));
        }

        compactActionButton(mQuickNewTabButton);
        compactActionButton(mQuickTabsButton);
        compactActionButton(mQuickHistoryButton);
        compactActionButton(mQuickBookmarkButton);

        mStatusView.setVisibility(View.GONE);
    }

    private void compactActionButton(@Nullable TextView button) {
        if (button == null) return;
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(4), dp(4), dp(4), dp(4));

        if (button.getLayoutParams() != null) {
            button.getLayoutParams().height = dp(32);
            button.setLayoutParams(button.getLayoutParams());
        }
    }

    private void applyFixedHeight(@Nullable View view, int heightDp) {
        if (view == null || view.getLayoutParams() == null) return;
        view.getLayoutParams().height = dp(heightDp);
        view.setLayoutParams(view.getLayoutParams());
    }

    private void renderReadyState() {
        ensureInitialTab();
        BrowserTab currentTab = getCurrentTab();
        String initialUrl = currentTab == null ? getDefaultBrowserUrl() : getTabUrlOrDefault(currentTab.url);
        if (currentTab != null) {
            currentTab.url = initialUrl;
        }

        updateDisplayedUrl(initialUrl);
        updateDisplayedTitle(currentTab == null ? mActivity.getString(R.string.browser_tab_title_default) : currentTab.title);
        mStatusView.setText("Browser ready. Opening home page.");
        if (mBrowserToolbar != null) {
            mBrowserToolbar.displayProgress(0);
        }
        updateNavigationState();

        if (!TextUtils.equals(initialUrl, mWebView.getUrl())) {
            loadNormalizedUrl(initialUrl, "Opening " + initialUrl);
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

        runOnMainThread(() -> loadNormalizedUrl(normalizedUrl, "Opening " + normalizedUrl));

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
            "element.scrollIntoView({block:'center',inline:'center'});" +
            "element.focus();" +
            "var tag=(element.tagName||'').toUpperCase();" +
            "var inputType=(element.type||'').toLowerCase();" +
            "var rect=element.getBoundingClientRect();" +
            "if (tag==='INPUT' && inputType==='file') {" +
            "  return JSON.stringify({ok:true,url:location.href,nativeTap:true,centerX:Math.max(1,rect.left + rect.width/2),centerY:Math.max(1,rect.top + rect.height/2)});" +
            "}" +
            "['mouseover','mousedown','mouseup','click'].forEach(function(type){element.dispatchEvent(new MouseEvent(type,{bubbles:true,cancelable:true,view:window}));});" +
            "if (typeof element.click === 'function') { element.click(); }" +
            "if (tag==='A' && element.href) { window.location.href = element.href; }" +
            "if (element.form && (tag==='BUTTON' || (tag==='INPUT' && (inputType==='submit' || inputType==='image')))) {" +
            "  if (typeof element.form.requestSubmit === 'function') { element.form.requestSubmit(element); } else { element.form.submit(); }" +
            "}" +
            "return JSON.stringify({ok:true,url:location.href});})()";

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

            if (data.optBoolean("nativeTap", false)) {
                float centerXCss = (float) data.optDouble("centerX", -1d);
                float centerYCss = (float) data.optDouble("centerY", -1d);
                if (centerXCss <= 0f || centerYCss <= 0f) {
                    return BrowserCommandResult.error(request.action, "INVALID_ELEMENT_GEOMETRY",
                        "Could not determine file input tap coordinates.");
                }

                CountDownLatch chooserLatch = new CountDownLatch(1);
                mPendingFileChooserLatch = chooserLatch;
                if (!performWebViewTap(centerXCss, centerYCss)) {
                    clearPendingFileChooserLatch(chooserLatch);
                    return BrowserCommandResult.error(request.action, "FILE_CHOOSER_NOT_OPENED",
                        "Native tap dispatch for file input failed.");
                }

                if (!awaitLatch(chooserLatch, Math.min(request.timeoutMs, 2000L))) {
                    clearPendingFileChooserLatch(chooserLatch);
                    return BrowserCommandResult.error(request.action, "FILE_CHOOSER_NOT_OPENED",
                        "File chooser did not open after tapping the file input.");
                }

                clearPendingFileChooserLatch(chooserLatch);
                data.remove("nativeTap");
                data.remove("centerX");
                data.remove("centerY");
            }

            return BrowserCommandResult.success(request.action, data);
        } catch (JSONException e) {
            return BrowserCommandResult.error(request.action, "SCRIPT_EXECUTION_FAILED", e.getMessage());
        }
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
    private BrowserCommandResult screenshot(@NonNull BrowserCommandRequest request) {
        final File outputFile;
        try {
            outputFile = resolveScreenshotOutputFile(request.output);
        } catch (IllegalArgumentException e) {
            return BrowserCommandResult.error(request.action, "INVALID_OUTPUT_PATH", e.getMessage());
        }

        AtomicReference<BrowserCommandResult> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        runOnMainThread(() -> captureScreenshot(outputFile, request.action, resultRef, latch));

        if (!awaitLatch(latch, request.timeoutMs)) {
            return BrowserCommandResult.error(request.action, "TIMEOUT", "Timed out waiting for screenshot capture.");
        }

        BrowserCommandResult result = resultRef.get();
        if (result == null) {
            return BrowserCommandResult.error(request.action, "SCREENSHOT_FAILED", "Screenshot capture did not return a result.");
        }

        return result;
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

    private void loadNormalizedUrl(@NonNull String normalizedUrl, @NonNull String status) {
        ensureInitialTab();
        updateStatus(status);
        updateDisplayedUrl(normalizedUrl);
        updateDisplayedTitle(normalizedUrl);
        BrowserTab currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.url = normalizedUrl;
            currentTab.title = normalizedUrl;
            currentTab.savedState = null;
        }
        Uri normalizedUri = Uri.parse(normalizedUrl);
        String previewText = TermuxBrowserWorkspace.readWorkspacePreviewText(normalizedUri);
        if (previewText != null) {
            mWebView.loadDataWithBaseURL(normalizedUrl, previewText, "text/html", "UTF-8", normalizedUrl);
        } else {
            mWebView.loadUrl(normalizedUrl);
        }
        persistTabsState();
        updateNavigationState();
    }

    private void updateDisplayedUrl(@Nullable String url) {
        String safeUrl = url == null ? "" : url;
        if (mBrowserToolbar != null) {
            mBrowserToolbar.setUrl(safeUrl);
        }
    }

    private void updateDisplayedTitle(@Nullable String title) {
        String safeTitle = TextUtils.isEmpty(title) ? mActivity.getString(R.string.browser_page_title_default) : title;
        if (mInteractiveChrome && mBrowserToolbar != null) {
            mBrowserToolbar.setTitle(safeTitle);
        } else {
            mHeaderPageTitleView.setText(safeTitle);
        }
    }

    private void updateNavigationState() {
        if (mBrowserToolbar != null) {
            mBrowserToolbar.invalidateActions();
        }
        renderTabStrip();
        updateQuickActions();
        renderTabsOverlay();
        renderLibraryOverlay();
        refreshToolbarMenu();
    }

    private void renderTabStrip() {
        if (mTabStrip == null) return;

        mTabStrip.removeAllViews();
        for (int i = 0; i < mTabs.size(); i++) {
            mTabStrip.addView(createTabStripChip(mTabs.get(i), i));
        }
        if (mInteractiveChrome) {
            mTabStrip.addView(createTabStripActionChip(
                mActivity.getString(R.string.browser_tabs_new),
                "#0F172A",
                "#334155",
                v -> createNewTab(true)
            ));
        }

        if (mTabStripScroll != null) {
            final int selectedIndex = mCurrentTabIndex;
            mTabStripScroll.post(() -> {
                if (selectedIndex < 0 || selectedIndex >= mTabStrip.getChildCount()) return;
                View selectedView = mTabStrip.getChildAt(selectedIndex);
                if (selectedView == null) return;
                int targetScroll = Math.max(0, selectedView.getLeft() - dp(12));
                mTabStripScroll.smoothScrollTo(targetScroll, 0);
            });
        }
    }

    private void updateQuickActions() {
        if (mQuickTabsButton != null) {
            mQuickTabsButton.setText(
                String.format(Locale.US, "%s %d", mActivity.getString(R.string.browser_quick_tabs), Math.max(1, mTabs.size()))
            );
            mQuickTabsButton.setAlpha(isTabsOverlayVisible() ? 1f : 0.88f);
        }
        if (mQuickHistoryButton != null) {
            mQuickHistoryButton.setAlpha(
                isLibraryOverlayVisible() && mLibraryMode == LibraryMode.HISTORY ? 1f : 0.88f
            );
        }
        if (mQuickBookmarkButton != null) {
            mQuickBookmarkButton.setText(
                isCurrentUrlBookmarked() ? R.string.browser_quick_bookmark_saved : R.string.browser_quick_bookmark
            );
            mQuickBookmarkButton.setAlpha(isCurrentUrlBookmarked() ? 1f : 0.85f);
        }
    }

    private void refreshToolbarMenu() {
        if (mBrowserToolbar == null) return;
        if (mMinimalChrome) {
            mBrowserToolbar.getDisplay().setMenuBuilder(null);
            return;
        }

        ArrayList<BrowserMenuItem> items = new ArrayList<>();
        items.add(new SimpleBrowserMenuItem(
            mActivity.getString(R.string.browser_menu_new_tab),
            0f,
            0,
            false,
            () -> {
                createNewTab(true);
                return Unit.INSTANCE;
            }
        ));
        items.add(new SimpleBrowserMenuItem(
            mActivity.getString(R.string.browser_menu_tabs),
            0f,
            0,
            false,
            () -> {
                showTabsOverlay();
                return Unit.INSTANCE;
            }
        ));
        items.add(new SimpleBrowserMenuItem(
            mActivity.getString(R.string.browser_menu_history),
            0f,
            0,
            false,
            () -> {
                showLibraryOverlay(LibraryMode.HISTORY);
                return Unit.INSTANCE;
            }
        ));
        items.add(new SimpleBrowserMenuItem(
            isCurrentUrlBookmarked()
                ? mActivity.getString(R.string.browser_menu_remove_bookmark)
                : mActivity.getString(R.string.browser_menu_add_bookmark),
            0f,
            0,
            false,
            () -> {
                toggleBookmark();
                return Unit.INSTANCE;
            }
        ));
        items.add(new SimpleBrowserMenuItem(
            mActivity.getString(R.string.browser_menu_bookmarks),
            0f,
            0,
            false,
            () -> {
                showLibraryOverlay(LibraryMode.BOOKMARKS);
                return Unit.INSTANCE;
            }
        ));

        if (mMinimalChrome && mActivity instanceof TermuxActivity) {
            items.add(new SimpleBrowserMenuItem(
                mActivity.getString(R.string.browser_menu_open_dev_mode),
                0f,
                0,
                false,
                () -> {
                    ((TermuxActivity) mActivity).requestModeSwitch("dev");
                    return Unit.INSTANCE;
                }
            ));
            items.add(new SimpleBrowserMenuItem(
                mActivity.getString(R.string.browser_menu_open_automation_mode),
                0f,
                0,
                false,
                () -> {
                    ((TermuxActivity) mActivity).requestModeSwitch("automation");
                    return Unit.INSTANCE;
                }
            ));
        }

        mBrowserToolbar.getDisplay().setMenuBuilder(
            new BrowserMenuBuilder(items, Collections.emptyMap(), false)
        );
    }

    private void showDailyModeMenu() {
        final boolean bookmarked = isCurrentUrlBookmarked();

        final ArrayList<MenuAction> actions = new ArrayList<>();
        actions.add(new MenuAction(R.string.browser_menu_new_tab, this::createNewTabFromMenu));
        actions.add(new MenuAction(R.string.browser_menu_tabs, this::showTabsOverlay));
        actions.add(new MenuAction(R.string.browser_menu_history, this::showHistoryFromMenu));
        actions.add(new MenuAction(
            bookmarked ? R.string.browser_menu_remove_bookmark : R.string.browser_menu_add_bookmark,
            this::toggleBookmark
        ));
        actions.add(new MenuAction(R.string.browser_menu_bookmarks, this::showBookmarksFromMenu));
        if (mActivity instanceof TermuxActivity) {
            actions.add(new MenuAction(
                R.string.browser_menu_open_dev_mode,
                () -> ((TermuxActivity) mActivity).requestModeSwitch("dev")
            ));
            actions.add(new MenuAction(
                R.string.browser_menu_open_automation_mode,
                () -> ((TermuxActivity) mActivity).requestModeSwitch("automation")
            ));
        }

        CharSequence[] labels = new CharSequence[actions.size()];
        for (int i = 0; i < actions.size(); i++) {
            labels[i] = mActivity.getString(actions.get(i).labelRes);
        }

        new AlertDialog.Builder(mActivity)
            .setItems(labels, (dialog, which) -> actions.get(which).runnable.run())
            .show();
    }

    private void createNewTabFromMenu() {
        createNewTab(true);
    }

    private void showHistoryFromMenu() {
        showLibraryOverlay(LibraryMode.HISTORY);
    }

    private void showBookmarksFromMenu() {
        showLibraryOverlay(LibraryMode.BOOKMARKS);
    }

    private void ensureInitialTab() {
        if (!mTabs.isEmpty()) return;
        BrowserTab tab = new BrowserTab(mNextTabId++, getDefaultBrowserUrl(), mActivity.getString(R.string.browser_tab_title_default));
        mTabs.add(tab);
        mCurrentTabIndex = 0;
    }

    @Nullable
    private BrowserTab getCurrentTab() {
        if (mCurrentTabIndex < 0 || mCurrentTabIndex >= mTabs.size()) return null;
        return mTabs.get(mCurrentTabIndex);
    }

    private void captureCurrentTabState() {
        BrowserTab currentTab = getCurrentTab();
        if (currentTab == null) return;

        String currentUrl = mWebView.getUrl();
        if (!TextUtils.isEmpty(currentUrl)) {
            currentTab.url = currentUrl;
        }

        Bundle state = new Bundle();
        mWebView.saveState(state);
        currentTab.savedState = state.isEmpty() ? null : state;
    }

    private void createNewTab(boolean selectTab) {
        BrowserTab tab = new BrowserTab(mNextTabId++, getDefaultBrowserUrl(), mActivity.getString(R.string.browser_tab_title_default));
        mTabs.add(tab);
        persistTabsState();
        if (selectTab) {
            switchToTab(mTabs.size() - 1);
        } else {
            renderTabsOverlay();
            updateNavigationState();
        }
    }

    private void switchToTab(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= mTabs.size()) return;

        if (targetIndex == mCurrentTabIndex) {
            hideTabsOverlay();
            return;
        }

        captureCurrentTabState();
        mCurrentTabIndex = targetIndex;
        BrowserTab targetTab = mTabs.get(targetIndex);
        hideTabsOverlay();

        Bundle savedState = targetTab.savedState == null ? null : new Bundle(targetTab.savedState);
        if (savedState != null) {
            mWebView.restoreState(savedState);
            updateDisplayedUrl(targetTab.url);
            updateDisplayedTitle(targetTab.title);
            updateStatus("Switched to tab");
        } else {
            loadNormalizedUrl(targetTab.url, "Opening " + targetTab.url);
        }

        persistTabsState();
        updateNavigationState();
    }

    private void closeTab(int index) {
        if (index < 0 || index >= mTabs.size()) return;

        mTabs.remove(index);
        if (mTabs.isEmpty()) {
            mCurrentTabIndex = -1;
            createNewTab(true);
            return;
        }

        if (mCurrentTabIndex >= mTabs.size()) {
            mCurrentTabIndex = mTabs.size() - 1;
        } else if (index < mCurrentTabIndex) {
            mCurrentTabIndex -= 1;
        } else if (index == mCurrentTabIndex) {
            BrowserTab currentTab = getCurrentTab();
            if (currentTab != null) {
                Bundle savedState = currentTab.savedState == null ? null : new Bundle(currentTab.savedState);
                if (savedState != null) {
                    mWebView.restoreState(savedState);
                    updateDisplayedUrl(currentTab.url);
                    updateDisplayedTitle(currentTab.title);
                } else {
                    loadNormalizedUrl(currentTab.url, "Opening " + currentTab.url);
                }
            }
        }

        persistTabsState();
        renderTabsOverlay();
        updateNavigationState();
    }

    private boolean isTabsOverlayVisible() {
        return mTabsOverlay != null && mTabsOverlay.getVisibility() == View.VISIBLE;
    }

    private void toggleTabsOverlay() {
        if (isTabsOverlayVisible()) {
            hideTabsOverlay();
        } else {
            showTabsOverlay();
        }
    }

    private void showTabsOverlay() {
        if (mTabsOverlay == null) return;
        hideLibraryOverlay();
        renderTabsOverlay();
        mTabsOverlay.setVisibility(View.VISIBLE);
        updateNavigationState();
    }

    private void hideTabsOverlay() {
        if (mTabsOverlay == null) return;
        mTabsOverlay.setVisibility(View.GONE);
        updateNavigationState();
    }

    private boolean isLibraryOverlayVisible() {
        return mLibraryOverlay != null && mLibraryOverlay.getVisibility() == View.VISIBLE;
    }

    private void showLibraryOverlay(@NonNull LibraryMode mode) {
        if (mLibraryOverlay == null) return;
        hideTabsOverlay();
        mLibraryMode = mode;
        renderLibraryOverlay();
        mLibraryOverlay.setVisibility(View.VISIBLE);
        updateNavigationState();
    }

    private void hideLibraryOverlay() {
        if (mLibraryOverlay == null) return;
        mLibraryOverlay.setVisibility(View.GONE);
        mLibraryMode = null;
        updateNavigationState();
    }

    private void renderTabsOverlay() {
        if (mTabsList == null) return;

        if (mTabsToggleBookmarkButton != null) {
            mTabsToggleBookmarkButton.setText(
                isCurrentUrlBookmarked()
                    ? R.string.browser_menu_remove_bookmark
                    : R.string.browser_menu_add_bookmark
            );
        }

        mTabsList.removeAllViews();
        for (int i = 0; i < mTabs.size(); i++) {
            BrowserTab tab = mTabs.get(i);
            mTabsList.addView(createTabRow(tab, i));
        }
    }

    private void renderLibraryOverlay() {
        if (mLibraryList == null || mLibraryTitleView == null) return;

        mLibraryList.removeAllViews();
        if (mLibraryMode == null) return;

        boolean bookmarks = mLibraryMode == LibraryMode.BOOKMARKS;
        mLibraryTitleView.setText(bookmarks ? R.string.browser_bookmarks_title : R.string.browser_history_title);
        ArrayList<BrowserEntry> source = bookmarks ? mBookmarkEntries : mHistoryEntries;

        if (source.isEmpty()) {
            AppCompatTextView emptyView = new AppCompatTextView(mActivity);
            emptyView.setText(bookmarks ? R.string.browser_library_empty_bookmarks : R.string.browser_library_empty_history);
            emptyView.setTextColor(Color.parseColor("#CBD5E1"));
            emptyView.setPadding(dp(8), dp(16), dp(8), dp(16));
            mLibraryList.addView(emptyView);
            return;
        }

        for (int i = 0; i < source.size(); i++) {
            mLibraryList.addView(createLibraryRow(source.get(i), bookmarks, i));
        }
    }

    @NonNull
    private View createTabStripChip(@NonNull BrowserTab tab, int index) {
        boolean selected = index == mCurrentTabIndex;

        LinearLayout chip = new LinearLayout(mActivity);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setPadding(dp(12), dp(10), dp(12), dp(10));
        chip.setMinimumWidth(dp(140));
        chip.setBackground(createRoundedBackground(
            selected ? "#1D4ED8" : "#0F172A",
            selected ? "#93C5FD" : "#334155",
            14
        ));
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        chipParams.rightMargin = dp(8);
        chip.setLayoutParams(chipParams);
        chip.setOnClickListener(v -> switchToTab(index));
        chip.setOnLongClickListener(v -> {
            showTabsOverlay();
            return true;
        });

        AppCompatTextView titleView = new AppCompatTextView(mActivity);
        titleView.setText(TextUtils.isEmpty(tab.title) ? mActivity.getString(R.string.browser_tab_title_default) : tab.title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        titleView.setMaxLines(1);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        AppCompatTextView urlView = new AppCompatTextView(mActivity);
        urlView.setText(getTabUrlOrDefault(tab.url));
        urlView.setTextColor(Color.parseColor(selected ? "#DBEAFE" : "#94A3B8"));
        urlView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        urlView.setMaxLines(1);
        urlView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        chip.addView(titleView);
        chip.addView(urlView);
        return chip;
    }

    @NonNull
    private View createTabStripActionChip(@NonNull String text, @NonNull String fillColor, @NonNull String strokeColor,
                                          @NonNull View.OnClickListener listener) {
        AppCompatTextView actionChip = new AppCompatTextView(mActivity);
        actionChip.setText(text);
        actionChip.setTextColor(Color.parseColor("#E2E8F0"));
        actionChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        actionChip.setPadding(dp(14), dp(14), dp(14), dp(14));
        actionChip.setBackground(createRoundedBackground(fillColor, strokeColor, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.rightMargin = dp(8);
        actionChip.setLayoutParams(params);
        actionChip.setOnClickListener(listener);
        return actionChip;
    }

    @NonNull
    private View createTabRow(@NonNull BrowserTab tab, int index) {
        LinearLayout row = new LinearLayout(mActivity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(createRoundedBackground(
            index == mCurrentTabIndex ? "#1E3A8A" : "#111827",
            index == mCurrentTabIndex ? "#93C5FD" : "#243244",
            16
        ));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.bottomMargin = dp(8);
        row.setLayoutParams(rowParams);
        row.setOnClickListener(v -> switchToTab(index));

        LinearLayout textHost = new LinearLayout(mActivity);
        textHost.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textHost.setLayoutParams(textParams);

        AppCompatTextView titleView = new AppCompatTextView(mActivity);
        titleView.setText(TextUtils.isEmpty(tab.title) ? mActivity.getString(R.string.browser_tab_title_default) : tab.title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        titleView.setMaxLines(1);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        AppCompatTextView urlView = new AppCompatTextView(mActivity);
        urlView.setText(getTabUrlOrDefault(tab.url));
        urlView.setTextColor(Color.parseColor("#CBD5E1"));
        urlView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        urlView.setMaxLines(1);
        urlView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        textHost.addView(titleView);
        textHost.addView(urlView);

        ImageButton closeButton = new ImageButton(mActivity);
        closeButton.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.browser_nav_close));
        closeButton.setContentDescription(mActivity.getString(R.string.browser_tab_close_desc));
        closeButton.setBackgroundColor(Color.TRANSPARENT);
        closeButton.setOnClickListener(v -> closeTab(index));

        row.addView(textHost);
        row.addView(closeButton);
        return row;
    }

    @NonNull
    private View createLibraryRow(@NonNull BrowserEntry entry, boolean bookmarks, int index) {
        LinearLayout row = new LinearLayout(mActivity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(createRoundedBackground("#111827", "#243244", 16));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.bottomMargin = dp(8);
        row.setLayoutParams(rowParams);
        row.setOnClickListener(v -> {
            hideLibraryOverlay();
            loadNormalizedUrl(entry.url, "Opening " + entry.url);
        });

        LinearLayout textHost = new LinearLayout(mActivity);
        textHost.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textHost.setLayoutParams(textParams);

        AppCompatTextView titleView = new AppCompatTextView(mActivity);
        titleView.setText(TextUtils.isEmpty(entry.title) ? entry.url : entry.title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        titleView.setMaxLines(1);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        AppCompatTextView urlView = new AppCompatTextView(mActivity);
        urlView.setText(entry.url);
        urlView.setTextColor(Color.parseColor("#CBD5E1"));
        urlView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        urlView.setMaxLines(1);
        urlView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        textHost.addView(titleView);
        textHost.addView(urlView);
        row.addView(textHost);

        if (bookmarks) {
            ImageButton removeButton = new ImageButton(mActivity);
            removeButton.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.browser_nav_close));
            removeButton.setContentDescription(mActivity.getString(R.string.browser_library_remove_desc));
            removeButton.setBackgroundColor(Color.TRANSPARENT);
            removeButton.setOnClickListener(v -> {
                mBookmarkEntries.remove(index);
                persistTabsState();
                renderLibraryOverlay();
                updateNavigationState();
            });
            row.addView(removeButton);
        }

        return row;
    }

    @NonNull
    private GradientDrawable createRoundedBackground(@NonNull String fillColor, @NonNull String strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(fillColor));
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), Color.parseColor(strokeColor));
        return drawable;
    }

    @NonNull
    private String getDefaultBrowserUrl() {
        return mActivity.getString(R.string.browser_default_url);
    }

    @NonNull
    private String getTabUrlOrDefault(@Nullable String url) {
        if (isBlankPage(url)) {
            return getDefaultBrowserUrl();
        }

        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (TextUtils.isEmpty(scheme)) {
            return url;
        }

        String normalizedScheme = scheme.toLowerCase(Locale.US);
        if ("http".equals(normalizedScheme) ||
            "https".equals(normalizedScheme) ||
            "file".equals(normalizedScheme) ||
            "content".equals(normalizedScheme)) {
            return url;
        }

        return getDefaultBrowserUrl();
    }

    private boolean isBlankPage(@Nullable String url) {
        return TextUtils.isEmpty(url) || "about:blank".equals(url);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            mActivity.getResources().getDisplayMetrics()
        ));
    }

    @NonNull
    private String getStateKey(@NonNull String suffix) {
        return mMode + "_" + mTargetContext + "_" + suffix;
    }

    @NonNull
    private SharedPreferences getTabsPreferences() {
        return mActivity.getSharedPreferences(PERSIST_PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void persistTabsState() {
        JSONArray tabsJson = new JSONArray();
        for (BrowserTab tab : mTabs) {
            JSONObject item = new JSONObject();
            try {
                item.put("id", tab.id);
                item.put("url", tab.url);
                item.put("title", tab.title);
            } catch (JSONException e) {
                Logger.logErrorExtended(LOG_TAG, "Failed to serialize tabs state\n" + e);
            }
            tabsJson.put(item);
        }

        getTabsPreferences()
            .edit()
            .putString(getStateKey(PERSIST_TABS), tabsJson.toString())
            .putString(getStateKey(PERSIST_HISTORY), serializeEntries(mHistoryEntries).toString())
            .putString(getStateKey(PERSIST_BOOKMARKS), serializeEntries(mBookmarkEntries).toString())
            .putInt(getStateKey(PERSIST_CURRENT), mCurrentTabIndex)
            .putLong(getStateKey(PERSIST_NEXT_ID), mNextTabId)
            .apply();
    }

    private void restorePersistentState() {
        String rawTabs = getTabsPreferences().getString(getStateKey(PERSIST_TABS), null);
        if (TextUtils.isEmpty(rawTabs)) return;

        try {
            JSONArray tabsJson = new JSONArray(rawTabs);
            if (tabsJson.length() == 0) return;

            mTabs.clear();
            long maxId = 0L;
            for (int i = 0; i < tabsJson.length(); i++) {
                JSONObject item = tabsJson.optJSONObject(i);
                if (item == null) continue;

                long id = item.optLong("id", i + 1L);
                String url = item.optString("url", getDefaultBrowserUrl());
                String title = item.optString("title", mActivity.getString(R.string.browser_tab_title_default));
                mTabs.add(new BrowserTab(id, url, title));
                maxId = Math.max(maxId, id);
            }

            if (mTabs.isEmpty()) return;

            mCurrentTabIndex = Math.min(
                Math.max(0, getTabsPreferences().getInt(getStateKey(PERSIST_CURRENT), 0)),
                mTabs.size() - 1
            );
            mNextTabId = Math.max(
                getTabsPreferences().getLong(getStateKey(PERSIST_NEXT_ID), maxId + 1L),
                maxId + 1L
            );
            restoreEntries(
                getTabsPreferences().getString(getStateKey(PERSIST_HISTORY), null),
                mHistoryEntries
            );
            restoreEntries(
                getTabsPreferences().getString(getStateKey(PERSIST_BOOKMARKS), null),
                mBookmarkEntries
            );
        } catch (JSONException e) {
            Logger.logErrorExtended(LOG_TAG, "Failed to restore persistent tabs state\n" + e);
        }
    }

    @NonNull
    private JSONArray serializeEntries(@NonNull ArrayList<BrowserEntry> entries) {
        JSONArray array = new JSONArray();
        for (BrowserEntry entry : entries) {
            JSONObject item = new JSONObject();
            try {
                item.put("url", entry.url);
                item.put("title", entry.title);
            } catch (JSONException e) {
                Logger.logErrorExtended(LOG_TAG, "Failed to serialize browser entry\n" + e);
            }
            array.put(item);
        }
        return array;
    }

    private void restoreEntries(@Nullable String raw, @NonNull ArrayList<BrowserEntry> target) {
        target.clear();
        if (TextUtils.isEmpty(raw)) return;

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                target.add(new BrowserEntry(
                    item.optString("url", getDefaultBrowserUrl()),
                    item.optString("title", mActivity.getString(R.string.browser_page_title_default))
                ));
            }
        } catch (JSONException e) {
            Logger.logErrorExtended(LOG_TAG, "Failed to restore browser entries\n" + e);
        }
    }

    private void recordHistoryEntry(@Nullable String url, @Nullable String title) {
        if (isBlankPage(url)) return;

        String safeTitle = TextUtils.isEmpty(title) ? url : title;
        if (!mHistoryEntries.isEmpty() && TextUtils.equals(mHistoryEntries.get(0).url, url)) {
            mHistoryEntries.get(0).title = safeTitle;
        } else {
            mHistoryEntries.add(0, new BrowserEntry(url, safeTitle));
        }

        while (mHistoryEntries.size() > MAX_HISTORY_ITEMS) {
            mHistoryEntries.remove(mHistoryEntries.size() - 1);
        }
        persistTabsState();
    }

    private boolean isCurrentUrlBookmarked() {
        BrowserTab currentTab = getCurrentTab();
        if (currentTab == null || isBlankPage(currentTab.url)) {
            return false;
        }

        for (BrowserEntry entry : mBookmarkEntries) {
            if (TextUtils.equals(entry.url, currentTab.url)) {
                return true;
            }
        }
        return false;
    }

    private void toggleBookmark() {
        BrowserTab currentTab = getCurrentTab();
        if (currentTab == null || isBlankPage(currentTab.url)) {
            return;
        }

        for (int i = 0; i < mBookmarkEntries.size(); i++) {
            BrowserEntry entry = mBookmarkEntries.get(i);
            if (TextUtils.equals(entry.url, currentTab.url)) {
                mBookmarkEntries.remove(i);
                updateStatus(mActivity.getString(R.string.browser_bookmark_removed));
                persistTabsState();
                updateNavigationState();
                return;
            }
        }

        mBookmarkEntries.add(0, new BrowserEntry(
            currentTab.url,
            TextUtils.isEmpty(currentTab.title) ? currentTab.url : currentTab.title
        ));
        updateStatus(mActivity.getString(R.string.browser_bookmark_added));
        persistTabsState();
        updateNavigationState();
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
    private File resolveScreenshotOutputFile(@Nullable String requestedPath) {
        File targetFile;
        if (TextUtils.isEmpty(requestedPath)) {
            targetFile = new File(TermuxConstants.TERMUX_APP.TERMUX_BROWSER_DOWNLOADS_DIR_PATH, buildDefaultScreenshotFileName());
        } else {
            String expandedPath = TermuxFileUtils.getExpandedTermuxPath(requestedPath.trim());
            targetFile = new File(expandedPath);
            if (!targetFile.isAbsolute()) {
                targetFile = new File(TermuxConstants.TERMUX_APP.TERMUX_BROWSER_WORKSPACE_DIR_PATH, requestedPath.trim());
            }
            if (requestedPath.endsWith("/") || (targetFile.exists() && targetFile.isDirectory())) {
                targetFile = new File(targetFile, buildDefaultScreenshotFileName());
            }
        }

        String name = targetFile.getName();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) {
            targetFile = new File(targetFile.getParentFile(), name + ".png");
        }

        try {
            File workspaceRoot = new File(TermuxConstants.TERMUX_APP.TERMUX_BROWSER_WORKSPACE_DIR_PATH);
            String workspaceCanonical = workspaceRoot.getCanonicalPath();
            String targetCanonical = targetFile.getCanonicalPath();
            if (!(targetCanonical.equals(workspaceCanonical) || targetCanonical.startsWith(workspaceCanonical + File.separator))) {
                throw new IllegalArgumentException("Screenshot output must stay inside the Termux workspace.");
            }
            return new File(targetCanonical);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to resolve screenshot output path.", e);
        }
    }

    @NonNull
    private String buildDefaultScreenshotFileName() {
        return "browser-screenshot-" + System.currentTimeMillis() + ".png";
    }

    private void captureScreenshot(@NonNull File outputFile,
                                   @NonNull String action,
                                   @NonNull AtomicReference<BrowserCommandResult> resultRef,
                                   @NonNull CountDownLatch latch) {
        if (mWebView.getWidth() <= 0 || mWebView.getHeight() <= 0) {
            resultRef.set(BrowserCommandResult.error(action, "BROWSER_SURFACE_UNAVAILABLE",
                "Target browser surface is not laid out for screenshot capture."));
            latch.countDown();
            return;
        }

        Runnable captureRunnable = () -> {
            Bitmap bitmap = null;
            try {
                File parent = outputFile.getParentFile();
                if (parent == null) {
                    resultRef.set(BrowserCommandResult.error(action, "INVALID_OUTPUT_PATH",
                        "Screenshot output path must include a parent directory."));
                    return;
                }
                if (!parent.exists() && !parent.mkdirs()) {
                    resultRef.set(BrowserCommandResult.error(action, "OUTPUT_DIRECTORY_CREATE_FAILED",
                        "Failed to create screenshot output directory."));
                    return;
                }

                int width = mWebView.getWidth();
                int height = mWebView.getHeight();
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                mWebView.draw(canvas);

                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                        resultRef.set(BrowserCommandResult.error(action, "SCREENSHOT_ENCODE_FAILED",
                            "Failed to encode PNG screenshot."));
                        return;
                    }
                }

                JSONObject result = new JSONObject();
                result.put("path", outputFile.getAbsolutePath());
                result.put("url", mWebView.getUrl());
                result.put("width", width);
                result.put("height", height);
                resultRef.set(BrowserCommandResult.success(action, result));
            } catch (Exception e) {
                resultRef.set(BrowserCommandResult.error(action, "SCREENSHOT_FAILED",
                    e.getMessage() != null ? e.getMessage() : e.toString()));
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                latch.countDown();
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mWebView.postVisualStateCallback(SystemClock.uptimeMillis(), new WebView.VisualStateCallback() {
                    @Override
                    public void onComplete(long requestId) {
                        captureRunnable.run();
                    }
                });
                return;
            } catch (Throwable throwable) {
                Logger.logWarnExtended(LOG_TAG, "postVisualStateCallback failed, falling back to delayed screenshot\n" + throwable);
            }
        }

        mMainHandler.postDelayed(captureRunnable, 100L);
    }

    private boolean awaitLatch(@NonNull CountDownLatch latch, long timeoutMs) {
        try {
            return latch.await(Math.max(timeoutMs, 1000L), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
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

    private boolean performWebViewTap(float x, float y) {
        AtomicBoolean dispatched = new AtomicBoolean(false);
        runOnMainThread(() -> {
            float scale = Math.max(1f, mWebView.getScale());
            float scaledX = x * scale;
            float scaledY = y * scale;
            float clampedX = Math.max(1f, Math.min(scaledX, Math.max(1, mWebView.getWidth() - 1)));
            float clampedY = Math.max(1f, Math.min(scaledY, Math.max(1, mWebView.getHeight() - 1)));
            int[] location = new int[2];
            mWebView.getLocationOnScreen(location);
            float windowX = location[0] + clampedX;
            float windowY = location[1] + clampedY;
            long downTime = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, windowX, windowY, 0);
            MotionEvent up = MotionEvent.obtain(downTime, downTime + 50L, MotionEvent.ACTION_UP, windowX, windowY, 0);
            try {
                boolean downHandled = mActivity.dispatchTouchEvent(down);
                boolean upHandled = mActivity.dispatchTouchEvent(up);
                dispatched.set(downHandled || upHandled);
            } finally {
                down.recycle();
                up.recycle();
            }
        });
        return dispatched.get();
    }

    private void clearPendingFileChooserLatch(@NonNull CountDownLatch latch) {
        if (mPendingFileChooserLatch == latch) {
            mPendingFileChooserLatch = null;
        }
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

    private void updateStatus(@NonNull String status) {
        mStatusView.setText(status);
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
            if (current.lastSignalAt.get() != signalAt) return;

            if (current.finished.compareAndSet(false, true)) {
                current.latch.countDown();
                mPendingPageLoad = null;
            }
        }, PAGE_SETTLE_DELAY_MS);
    }

    private boolean matchesPendingOpen(@NonNull PendingPageLoad pendingPageLoad, @NonNull String url) {
        String expected = pendingPageLoad.requestedUrl;
        return url.equals(expected) || url.startsWith(expected) || expected.startsWith(url);
    }

    private void resetPendingLoadIfNewNavigation(@Nullable String url) {
        PendingPageLoad pendingPageLoad = mPendingPageLoad;
        if (pendingPageLoad == null || pendingPageLoad.finished.get()) return;
        if (url == null || matchesPendingOpen(pendingPageLoad, url)) return;

        pendingPageLoad.url.set(url);
        pendingPageLoad.lastSignalAt.set(SystemClock.elapsedRealtime());
    }

    private void clearConsoleErrors() {
        synchronized (mConsoleErrors) {
            mConsoleErrors.clear();
        }
    }

    private final class RuntimeWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            updateDisplayedUrl(url);
            updateDisplayedTitle(url);
            updateStatus("Loading " + url);
            BrowserTab currentTab = getCurrentTab();
            if (currentTab != null) {
                currentTab.url = url;
            }
            updateNavigationState();
            resetPendingLoadIfNewNavigation(url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            updateDisplayedUrl(url);
            updateStatus("Ready: " + url);
            BrowserTab currentTab = getCurrentTab();
            if (currentTab != null) {
                currentTab.url = url;
                currentTab.savedState = null;
            }
            updateNavigationState();
            finishPendingLoad(url, null);
            super.onPageFinished(view, url);
            recordHistoryEntry(url, view.getTitle());
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

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (request.isForMainFrame()) {
                finishPendingLoad(view.getUrl(), "HTTP error " + errorResponse.getStatusCode());
            }
            super.onReceivedHttpError(view, request, errorResponse);
        }
    }

    private final class RuntimeWebChromeClient extends WebChromeClient {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            updateDisplayedTitle(TextUtils.isEmpty(title) ? view.getUrl() : title);
            BrowserTab currentTab = getCurrentTab();
            if (currentTab != null && !TextUtils.isEmpty(title)) {
                currentTab.title = title;
            }
            persistTabsState();
            if (mPendingPageLoad != null) {
                mPendingPageLoad.title.set(title);
            }
            super.onReceivedTitle(view, title);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (mBrowserToolbar != null) {
                mBrowserToolbar.displayProgress(newProgress);
            }
            updateNavigationState();
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                pushConsoleError(consoleMessage);
            }
            return super.onConsoleMessage(consoleMessage);
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (mFileChooserCallback != null) {
                mFileChooserCallback.onReceiveValue(null);
            }

            updateStatus("Opening file chooser");
            mFileChooserCallback = filePathCallback;
            if (mPendingFileChooserLatch != null) {
                mPendingFileChooserLatch.countDown();
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, TermuxBrowserWorkspace.getWorkspaceDocumentsUri());
            }

            try {
                mActivity.startActivityForResult(intent, FILE_PICKER_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                if (mFileChooserCallback != null) {
                    mFileChooserCallback.onReceiveValue(null);
                    mFileChooserCallback = null;
                }
                return false;
            }

            return true;
        }
    }

    private final class BrowserDownloadListener implements DownloadListener {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
            new Thread(() -> downloadToWorkspace(url, contentDisposition, mimeType), "termux-browser-download").start();
        }
    }

    private void downloadToWorkspace(@NonNull String urlString, @Nullable String contentDisposition, @Nullable String mimeType) {
        String fileName = android.webkit.URLUtil.guessFileName(urlString, contentDisposition, mimeType);
        File downloadsDir = new File(com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_BROWSER_DOWNLOADS_DIR_PATH);
        File targetFile = new File(downloadsDir, fileName);

        try (BufferedInputStream inputStream = openDownloadStream(urlString);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            mMainHandler.post(() -> updateStatus("Downloaded to " + targetFile.getAbsolutePath()));
        } catch (Exception e) {
            Logger.logErrorExtended(LOG_TAG, "Download failed\n" + e);
            mMainHandler.post(() -> updateStatus("Download failed: " + e.getMessage()));
        }
    }

    @NonNull
    private BufferedInputStream openDownloadStream(@NonNull String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        String cookie = CookieManager.getInstance().getCookie(urlString);
        if (!TextUtils.isEmpty(cookie)) {
            connection.setRequestProperty("Cookie", cookie);
        }
        return new BufferedInputStream(connection.getInputStream());
    }

    private static final class PendingPageLoad {
        final String requestedUrl;
        final CountDownLatch latch;
        final AtomicReference<String> url;
        final AtomicReference<String> title;
        final AtomicReference<String> error;
        final AtomicBoolean finished = new AtomicBoolean(false);
        final AtomicReference<Long> lastSignalAt = new AtomicReference<>(0L);

        PendingPageLoad(@NonNull String requestedUrl, @NonNull CountDownLatch latch, @NonNull AtomicReference<String> url,
                        @NonNull AtomicReference<String> title, @NonNull AtomicReference<String> error) {
            this.requestedUrl = requestedUrl;
            this.latch = latch;
            this.url = url;
            this.title = title;
            this.error = error;
        }
    }

    private static final class MenuAction {
        final int labelRes;
        @NonNull final Runnable runnable;

        MenuAction(int labelRes, @NonNull Runnable runnable) {
            this.labelRes = labelRes;
            this.runnable = runnable;
        }
    }

    private static final class BrowserTab {
        final long id;
        String url;
        String title;
        @Nullable Bundle savedState;

        BrowserTab(long id, @NonNull String url, @NonNull String title) {
            this.id = id;
            this.url = url;
            this.title = title;
        }
    }

    private static final class BrowserEntry {
        final String url;
        String title;

        BrowserEntry(@NonNull String url, @NonNull String title) {
            this.url = url;
            this.title = title;
        }
    }

    private enum LibraryMode {
        HISTORY,
        BOOKMARKS
    }

    private final class TabsAction implements Toolbar.Action {
        @Override
        public kotlin.jvm.functions.Function0<Boolean> getVisible() {
            return () -> true;
        }

        @Override
        public kotlin.jvm.functions.Function0<Boolean> getAutoHide() {
            return () -> false;
        }

        @Override
        public kotlin.jvm.functions.Function0<Integer> getWeight() {
            return () -> 0;
        }

        @Override
        public View createView(ViewGroup parent) {
            AppCompatTextView counter = new AppCompatTextView(parent.getContext());
            int size = dp(40);
            counter.setMinWidth(size);
            counter.setMinHeight(size);
            counter.setGravity(android.view.Gravity.CENTER);
            counter.setTextColor(Color.WHITE);
            counter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            counter.setBackgroundResource(android.R.drawable.btn_default_small);
            counter.setOnClickListener(v -> toggleTabsOverlay());
            return counter;
        }

        @Override
        public void bind(View view) {
            AppCompatTextView counter = (AppCompatTextView) view;
            counter.setText(String.format(Locale.US, "%d", Math.max(1, mTabs.size())));
            counter.setAlpha(isTabsOverlayVisible() ? 0.7f : 1f);
        }
    }

    private final class NewTabAction implements Toolbar.Action {
        @Override
        public kotlin.jvm.functions.Function0<Boolean> getVisible() {
            return () -> true;
        }

        @Override
        public kotlin.jvm.functions.Function0<Boolean> getAutoHide() {
            return () -> false;
        }

        @Override
        public kotlin.jvm.functions.Function0<Integer> getWeight() {
            return () -> 0;
        }

        @Override
        public View createView(ViewGroup parent) {
            AppCompatTextView button = new AppCompatTextView(parent.getContext());
            int size = dp(40);
            button.setMinWidth(size);
            button.setMinHeight(size);
            button.setGravity(android.view.Gravity.CENTER);
            button.setText("+");
            button.setTextColor(Color.WHITE);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            button.setContentDescription(mActivity.getString(R.string.browser_action_new_tab_desc));
            button.setOnClickListener(v -> createNewTab(true));
            return button;
        }

        @Override
        public void bind(View view) {
            view.setAlpha(1f);
        }
    }

}
