package com.termux.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.webkit.WebView;
import com.google.android.material.button.MaterialButton;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;
import com.termux.app.api.file.FileReceiverActivity;
import com.termux.app.terminal.TermuxActivityRootView;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.io.TermuxTerminalExtraKeys;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY;
import com.termux.app.activities.HelpActivity;
import com.termux.app.activities.SettingsActivity;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.app.browser.TermuxBrowserWorkspace;
import com.termux.app.config.ModeConfig;
import com.termux.app.config.ModeConfigManager;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.io.TerminalToolbarViewPager;
import com.termux.app.terminal.TermuxTerminalViewClient;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.shared.theme.NightMode;
import com.termux.shared.view.ViewUtils;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A terminal emulator activity.
 * <p/>
 * See
 * <ul>
 * <li>http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android</li>
 * <li>https://code.google.com/p/android/issues/detail?id=6426</li>
 * </ul>
 * about memory leaks.
 */
public final class TermuxActivity extends AppCompatActivity implements ServiceConnection {

    /**
     * The connection to the {@link TermuxService}. Requested in {@link #onCreate(Bundle)} with a call to
     * {@link #bindService(Intent, ServiceConnection, int)}, and obtained and stored in
     * {@link #onServiceConnected(ComponentName, IBinder)}.
     */
    TermuxService mTermuxService;

    /**
     * The {@link TerminalView} shown in  {@link TermuxActivity} that displays the terminal.
     */
    TerminalView mTerminalView;

    /**
     *  The {@link TerminalViewClient} interface implementation to allow for communication between
     *  {@link TerminalView} and {@link TermuxActivity}.
     */
    TermuxTerminalViewClient mTermuxTerminalViewClient;

    /**
     *  The {@link TerminalSessionClient} interface implementation to allow for communication between
     *  {@link TerminalSession} and {@link TermuxActivity}.
     */
    TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;

    /**
     * Termux app shared preferences manager.
     */
    private TermuxAppSharedPreferences mPreferences;

    /**
     * Termux app SharedProperties loaded from termux.properties
     */
    private TermuxAppSharedProperties mProperties;

    /**
     * The root view of the {@link TermuxActivity}.
     */
    TermuxActivityRootView mTermuxActivityRootView;

    /**
     * The space at the bottom of {@link @mTermuxActivityRootView} of the {@link TermuxActivity}.
     */
    View mTermuxActivityBottomSpaceView;

    /**
     * The terminal extra keys view.
     */
    ExtraKeysView mExtraKeysView;

    /**
     * The client for the {@link #mExtraKeysView}.
     */
    TermuxTerminalExtraKeys mTermuxTerminalExtraKeys;

    /**
     * The termux sessions list controller.
     */
    TermuxSessionsListViewController mTermuxSessionListViewController;

    /**
     * The {@link TermuxActivity} broadcast receiver for various things like terminal style configuration changes.
     */
    private final BroadcastReceiver mTermuxActivityBroadcastReceiver = new TermuxActivityBroadcastReceiver();

    /**
     * The last toast shown, used cancel current toast before showing new in {@link #showToast(String, boolean)}.
     */
    Toast mLastToast;

    /**
     * If between onResume() and onStop(). Note that only one session is in the foreground of the terminal view at the
     * time, so if the session causing a change is not in the foreground it should probably be treated as background.
     */
    private boolean mIsVisible;

    /**
     * If onResume() was called after onCreate().
     */
    private boolean mIsOnResumeAfterOnCreate = false;

    /**
     * If activity was restarted like due to call to {@link #recreate()} after receiving
     * {@link TERMUX_ACTIVITY#ACTION_RELOAD_STYLE}, system dark night mode was changed or activity
     * was killed by android.
     */
    private boolean mIsActivityRecreated = false;

    /**
     * The {@link TermuxActivity} is in an invalid state and must not be run.
     */
    private boolean mIsInvalidState;

    private int mNavBarHeight;

    private float mTerminalToolbarDefaultHeight;
    private TermuxBrowserController mControlBrowserController;
    private TermuxBrowserController mWorkspaceBrowserController;
    private String mCurrentMode;
    private View mGlobalModeBarView;
    private View mGlobalModeHeaderView;
    private View mGlobalModeButtonRowView;
    private View mGlobalStatusBarView;
    private View mDevSurfaceSwitcherView;
    private TextView mGlobalAppTitleView;
    private TextView mModeTitleView;
    private TextView mModeSummaryView;
    private MaterialButton mDailyModeButton;
    private MaterialButton mDevModeButton;
    private MaterialButton mAutomationModeButton;
    private MaterialButton mDevSurfacePreviewButton;
    private MaterialButton mDevSurfaceTerminalButton;
    private MaterialButton mDevSurfaceControllerButton;
    private View mDevTerminalActionsView;
    private View mDevTerminalActionButtonsView;
    private MaterialButton mDevTerminalSessionsButton;
    private MaterialButton mDevTerminalNewSessionButton;
    private MaterialButton mDevTerminalToggleButton;
    private boolean mDevTerminalActionsExpanded;
    private boolean mDevTerminalActionsPositionInitialized;
    private float mDevTerminalActionsSavedX;
    private float mDevTerminalActionsSavedY;
    @NonNull private String mDevActiveSurface = "preview";


    private static final int CONTEXT_MENU_SELECT_URL_ID = 0;
    private static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;
    private static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 10;
    private static final int CONTEXT_MENU_AUTOFILL_USERNAME = 11;
    private static final int CONTEXT_MENU_AUTOFILL_PASSWORD = 2;
    private static final int CONTEXT_MENU_RESET_TERMINAL_ID = 3;
    private static final int CONTEXT_MENU_KILL_PROCESS_ID = 4;
    private static final int CONTEXT_MENU_STYLING_ID = 5;
    private static final int CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON = 6;
    private static final int CONTEXT_MENU_HELP_ID = 7;
    private static final int CONTEXT_MENU_SETTINGS_ID = 8;
    private static final int CONTEXT_MENU_REPORT_ID = 9;

    private static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";
    private static final String ARG_ACTIVITY_RECREATED = "activity_recreated";
    private static final String ARG_DEV_TERMINAL_ACTIONS_EXPANDED = "dev_terminal_actions_expanded";
    private static final String ARG_DEV_TERMINAL_ACTIONS_POSITION_INITIALIZED = "dev_terminal_actions_position_initialized";
    private static final String ARG_DEV_TERMINAL_ACTIONS_X = "dev_terminal_actions_x";
    private static final String ARG_DEV_TERMINAL_ACTIONS_Y = "dev_terminal_actions_y";

    private static final String LOG_TAG = "TermuxActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(LOG_TAG, "onCreate");
        mIsOnResumeAfterOnCreate = true;

        if (savedInstanceState != null)
            mIsActivityRecreated = savedInstanceState.getBoolean(ARG_ACTIVITY_RECREATED, false);
        if (savedInstanceState != null) {
            mDevTerminalActionsExpanded = savedInstanceState.getBoolean(ARG_DEV_TERMINAL_ACTIONS_EXPANDED, false);
            mDevTerminalActionsPositionInitialized = savedInstanceState.getBoolean(ARG_DEV_TERMINAL_ACTIONS_POSITION_INITIALIZED, false);
            mDevTerminalActionsSavedX = savedInstanceState.getFloat(ARG_DEV_TERMINAL_ACTIONS_X, 0f);
            mDevTerminalActionsSavedY = savedInstanceState.getFloat(ARG_DEV_TERMINAL_ACTIONS_Y, 0f);
        }

        // Delete ReportInfo serialized object files from cache older than 14 days
        ReportActivity.deleteReportInfoFilesOlderThanXDays(this, 14, false);

        // Load Termux app SharedProperties from disk
        mProperties = TermuxAppSharedProperties.getProperties();
        reloadProperties();

        setActivityTheme();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_termux);

        // Load termux shared preferences
        // This will also fail if TermuxConstants.TERMUX_PACKAGE_NAME does not equal applicationId
        mPreferences = TermuxAppSharedPreferences.build(this, true);
        if (mPreferences == null) {
            // An AlertDialog should have shown to kill the app, so we don't continue running activity code
            mIsInvalidState = true;
            return;
        }

        setMargins();
        TermuxBrowserWorkspace.ensureRuntimeSetup(this);
        initModeConfig(getIntent());

        mTermuxActivityRootView = findViewById(R.id.activity_termux_root_view);
        mTermuxActivityRootView.setActivity(this);
        mTermuxActivityBottomSpaceView = findViewById(R.id.activity_termux_bottom_space_view);
        mTermuxActivityRootView.setOnApplyWindowInsetsListener(new TermuxActivityRootView.WindowInsetsListener());

        View content = findViewById(android.R.id.content);
        content.setOnApplyWindowInsetsListener((v, insets) -> {
            mNavBarHeight = insets.getSystemWindowInsetBottom();
            return insets;
        });

        if (mProperties.isUsingFullScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setTermuxTerminalViewAndClients();
        setTermuxBrowserView();
        restoreBrowserState(savedInstanceState);
        setModeSwitcherView();
        setDevSurfaceSwitcherView();
        setTerminalToolbarView(savedInstanceState);
        setDevTerminalActionsView();
        applyModeUi();

        setSettingsButtonView();

        setNewSessionButtonView();

        setToggleKeyboardView();

        registerForContextMenu(mTerminalView);

        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);

        try {
            // Start the {@link TermuxService} and make it run regardless of who is bound to it
            Intent serviceIntent = new Intent(this, TermuxService.class);
            startService(serviceIntent);

            // Attempt to bind to the service, this will call the {@link #onServiceConnected(ComponentName, IBinder)}
            // callback if it succeeds.
            if (!bindService(serviceIntent, this, 0))
                throw new RuntimeException("bindService() failed");
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG,"TermuxActivity failed to start TermuxService", e);
            Logger.showToast(this,
                getString(e.getMessage() != null && e.getMessage().contains("app is in background") ?
                    R.string.error_termux_service_start_failed_bg : R.string.error_termux_service_start_failed_general),
                true);
            mIsInvalidState = true;
            return;
        }

        // Send the {@link TermuxConstants#BROADCAST_TERMUX_OPENED} broadcast to notify apps that Termux
        // app has been opened.
        TermuxUtils.sendTermuxOpenedBroadcast(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        Logger.logDebug(LOG_TAG, "onStart");

        if (mIsInvalidState) return;

        mIsVisible = true;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStart();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStart();

        if (mPreferences.isTerminalMarginAdjustmentEnabled())
            addTermuxActivityRootViewGlobalLayoutListener();

        registerTermuxActivityBroadcastReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();

        Logger.logVerbose(LOG_TAG, "onResume");

        if (mIsInvalidState) return;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onResume();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onResume();

        TermuxBrowserWorkspace.ensureRuntimeSetup(this);

        if (mControlBrowserController != null)
            mControlBrowserController.onResume();
        if (mWorkspaceBrowserController != null)
            mWorkspaceBrowserController.onResume();

        // Check if a crash happened on last run of the app or if a plugin crashed and show a
        // notification with the crash details if it did
        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(this, LOG_TAG);

        mIsOnResumeAfterOnCreate = false;
    }

    @Override
    protected void onPause() {
        if (mControlBrowserController != null)
            mControlBrowserController.onPause();
        if (mWorkspaceBrowserController != null)
            mWorkspaceBrowserController.onPause();

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        updateModeFromIntent(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Logger.logDebug(LOG_TAG, "onStop");

        if (mIsInvalidState) return;

        mIsVisible = false;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStop();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStop();

        removeTermuxActivityRootViewGlobalLayoutListener();

        unregisterTermuxActivityBroadcastReceiver();
        getDrawer().closeDrawers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Logger.logDebug(LOG_TAG, "onDestroy");

        if (mIsInvalidState) return;

        if (mTermuxService != null) {
            // Do not leave service and session clients with references to activity.
            mTermuxService.unsetTermuxTerminalSessionClient();
            mTermuxService = null;
        }

        try {
            unbindService(this);
        } catch (Exception e) {
            // ignore.
        }

        destroyBrowserControllers();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        Logger.logVerbose(LOG_TAG, "onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);
        saveTerminalToolbarTextInput(savedInstanceState);
        if (mControlBrowserController != null) {
            mControlBrowserController.saveInstanceState(savedInstanceState);
        }
        if (mWorkspaceBrowserController != null) {
            mWorkspaceBrowserController.saveInstanceState(savedInstanceState);
        }
        savedInstanceState.putBoolean(ARG_ACTIVITY_RECREATED, true);
        savedInstanceState.putBoolean(ARG_DEV_TERMINAL_ACTIONS_EXPANDED, mDevTerminalActionsExpanded);
        savedInstanceState.putBoolean(ARG_DEV_TERMINAL_ACTIONS_POSITION_INITIALIZED, mDevTerminalActionsPositionInitialized);
        savedInstanceState.putFloat(ARG_DEV_TERMINAL_ACTIONS_X, mDevTerminalActionsSavedX);
        savedInstanceState.putFloat(ARG_DEV_TERMINAL_ACTIONS_Y, mDevTerminalActionsSavedY);
    }

    /**
     * Part of the {@link ServiceConnection} interface. The service is bound with
     * {@link #bindService(Intent, ServiceConnection, int)} in {@link #onCreate(Bundle)} which will cause a call to this
     * callback method.
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Logger.logDebug(LOG_TAG, "onServiceConnected");

        mTermuxService = ((TermuxService.LocalBinder) service).service;

        setTermuxSessionsListView();

        final Intent intent = getIntent();
        setIntent(null);

        if (needsBootstrapSetup()) {
            if (mIsVisible) {
                clearStaleTermuxSessionsForBootstrap();
                ensureBootstrapAndLaunchSession(intent);
            } else {
                finishActivityIfNotFinishing();
            }
        } else if (mTermuxService.isTermuxSessionsEmpty()) {
            if (mIsVisible) {
                ensureBootstrapAndLaunchSession(intent);
            } else {
                // The service connected while not in foreground - just bail out.
                finishActivityIfNotFinishing();
            }
        } else {
            // If termux was started from launcher "New session" shortcut and activity is recreated,
            // then the original intent will be re-delivered, resulting in a new session being re-added
            // each time.
            if (!mIsActivityRecreated && intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {
                // Android 7.1 app shortcut from res/xml/shortcuts.xml.
                boolean isFailSafe = intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
                mTermuxTerminalSessionActivityClient.addNewSession(isFailSafe, null);
            } else {
                mTermuxTerminalSessionActivityClient.setCurrentSession(mTermuxTerminalSessionActivityClient.getCurrentStoredSessionOrLast());
            }
        }

        // Update the {@link TerminalSession} and {@link TerminalEmulator} clients.
        mTermuxService.setTermuxTerminalSessionClient(mTermuxTerminalSessionActivityClient);
        updateDevTerminalActionsUi("dev".equals(mCurrentMode) && "terminal".equals(mDevActiveSurface));
    }

    private boolean needsBootstrapSetup() {
        return TermuxFileUtils.isTermuxPrefixDirectoryAccessible(false, false) != null ||
            TermuxFileUtils.isTermuxPrefixDirectoryEmpty();
    }

    private void clearStaleTermuxSessionsForBootstrap() {
        if (mTermuxService == null) return;

        ArrayList<TermuxSession> staleSessions = new ArrayList<>(mTermuxService.getTermuxSessions());
        for (TermuxSession termuxSession : staleSessions) {
            if (termuxSession == null) continue;
            termuxSession.killIfExecuting(this, false);
        }
        mTermuxService.getTermuxSessions().clear();
    }

    private void ensureBootstrapAndLaunchSession(@Nullable Intent intent) {
        TermuxInstaller.setupBootstrapIfNeeded(TermuxActivity.this, () -> {
            if (mTermuxService == null) return;
            try {
                boolean launchFailsafe = false;
                if (intent != null && intent.getExtras() != null) {
                    launchFailsafe = intent.getExtras().getBoolean(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
                }
                mTermuxTerminalSessionActivityClient.addNewSession(launchFailsafe, null);
            } catch (WindowManager.BadTokenException e) {
                // Activity finished - ignore.
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Logger.logDebug(LOG_TAG, "onServiceDisconnected");

        // Respect being stopped from the {@link TermuxService} notification action.
        finishActivityIfNotFinishing();
    }






    private void reloadProperties() {
        mProperties.loadTermuxPropertiesFromDisk();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadProperties();
    }



    private void setActivityTheme() {
        // Update NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());

        // Set activity night mode. If NightMode.SYSTEM is set, then android will automatically
        // trigger recreation of activity when uiMode/dark mode configuration is changed so that
        // day or night theme takes affect.
        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);
    }

    private void setMargins() {
        RelativeLayout relativeLayout = findViewById(R.id.activity_termux_root_relative_layout);
        int marginHorizontal = mProperties.getTerminalMarginHorizontal();
        int marginVertical = mProperties.getTerminalMarginVertical();
        ViewUtils.setLayoutMarginsInDp(relativeLayout, marginHorizontal, marginVertical, marginHorizontal, marginVertical);
    }



    public void addTermuxActivityRootViewGlobalLayoutListener() {
        getTermuxActivityRootView().getViewTreeObserver().addOnGlobalLayoutListener(getTermuxActivityRootView());
    }

    public void removeTermuxActivityRootViewGlobalLayoutListener() {
        if (getTermuxActivityRootView() != null)
            getTermuxActivityRootView().getViewTreeObserver().removeOnGlobalLayoutListener(getTermuxActivityRootView());
    }



    private void setTermuxTerminalViewAndClients() {
        // Set termux terminal view and session clients
        mTermuxTerminalSessionActivityClient = new TermuxTerminalSessionActivityClient(this);
        mTermuxTerminalViewClient = new TermuxTerminalViewClient(this, mTermuxTerminalSessionActivityClient);

        // Set termux terminal view
        mTerminalView = findViewById(R.id.terminal_view);
        mTerminalView.setTerminalViewClient(mTermuxTerminalViewClient);

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onCreate();

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onCreate();
    }

    private void setTermuxBrowserView() {
        destroyBrowserControllers();

        LinearLayout controlContainer = findViewById(R.id.control_browser_container);
        LinearLayout workspaceContainer = findViewById(R.id.workspace_browser_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        controlContainer.removeAllViews();
        workspaceContainer.removeAllViews();

        View controlPanel = inflater.inflate(R.layout.view_browser_panel, controlContainer, false);
        View workspacePanel = inflater.inflate(R.layout.view_browser_panel, workspaceContainer, false);
        controlContainer.addView(controlPanel);
        workspaceContainer.addView(workspacePanel);

        mControlBrowserController = buildBrowserController("control_webview", controlPanel);
        mWorkspaceBrowserController = buildBrowserController("workspace_webview", workspacePanel);
    }

    private void restoreBrowserState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        if (mControlBrowserController != null) {
            mControlBrowserController.restoreInstanceState(savedInstanceState);
        }
        if (mWorkspaceBrowserController != null) {
            mWorkspaceBrowserController.restoreInstanceState(savedInstanceState);
        }
    }

    private void initModeConfig(@Nullable Intent intent) {
        ModeConfig modeConfig = ModeConfigManager.getModeConfig();
        mCurrentMode = modeConfig.getDefaultMode();
        updateModeFromIntent(intent);
    }

    private void updateModeFromIntent(@Nullable Intent intent) {
        if (intent == null) return;

        String requestedMode = intent.getStringExtra(TermuxBrowserWorkspace.EXTRA_TARGET_MODE);
        if (requestedMode == null || requestedMode.trim().isEmpty()) return;

        ModeConfig modeConfig = ModeConfigManager.getModeConfig();
        if (!modeConfig.isModeEnabled(requestedMode)) return;
        if (requestedMode.equals(mCurrentMode)) return;

        mCurrentMode = requestedMode;
        if (findViewById(R.id.control_browser_container) != null) {
            setTermuxBrowserView();
        }
        applyModeUi();
    }

    private void applyModeUi() {
        ModeConfig modeConfig = ModeConfigManager.getModeConfig();
        String layout = modeConfig.getLayout(mCurrentMode);
        String browserRuntime = modeConfig.getBrowserRuntime(mCurrentMode);

        View workspaceBrowserContainer = findViewById(R.id.workspace_browser_container);
        View terminalHostContainer = findViewById(R.id.activity_termux_root_relative_layout);
        View controlBrowserContainer = findViewById(R.id.control_browser_container);
        TextView statusLeftView = findViewById(R.id.status_left_view);
        TextView statusRightView = findViewById(R.id.status_right_view);

        boolean devLayout = "three_panel".equals(layout);
        boolean dailyLayout = "single_webview".equals(layout);
        boolean headlessLayout = "none".equals(layout) || "headless".equals(browserRuntime);

        if (devLayout) {
            applyDevSurfaceVisibility(workspaceBrowserContainer, terminalHostContainer, controlBrowserContainer);
        } else {
            if (workspaceBrowserContainer != null) {
                workspaceBrowserContainer.setVisibility(View.GONE);
            }
            if (terminalHostContainer != null) {
                terminalHostContainer.setVisibility(headlessLayout ? View.GONE : View.GONE);
            }
            if (controlBrowserContainer != null) {
                controlBrowserContainer.setVisibility(dailyLayout ? View.VISIBLE : View.GONE);
            }
            updateDevSurfaceSwitcherUi(false);
            updateDevTerminalActionsUi(false);
        }

        if (statusLeftView != null) {
            statusLeftView.setText(getString(R.string.status_left_default));
        }
        if (statusRightView != null) {
            statusRightView.setText(getModeStatusRes(mCurrentMode));
        }

        applyTerminalToolbarForMode();
        updateModeSwitcherUi();
    }

    private void applyDevSurfaceVisibility(@Nullable View workspaceBrowserContainer,
                                           @Nullable View terminalHostContainer,
                                           @Nullable View controlBrowserContainer) {
        bindDevTerminalActionsView(null);
        applyDevBrowserContainerOffsets(workspaceBrowserContainer, controlBrowserContainer);
        if (workspaceBrowserContainer != null) {
            workspaceBrowserContainer.setVisibility("preview".equals(mDevActiveSurface) ? View.VISIBLE : View.INVISIBLE);
        }
        if (terminalHostContainer != null) {
            terminalHostContainer.setVisibility("terminal".equals(mDevActiveSurface) ? View.VISIBLE : View.INVISIBLE);
        }
        if (controlBrowserContainer != null) {
            controlBrowserContainer.setVisibility("controller".equals(mDevActiveSurface) ? View.VISIBLE : View.INVISIBLE);
        }
        applyTerminalSurfaceOffset();
        updateDevSurfaceSwitcherUi(true);
        updateDevTerminalActionsUi("terminal".equals(mDevActiveSurface));
    }

    private void applyDevBrowserContainerOffsets(@Nullable View workspaceBrowserContainer,
                                                 @Nullable View controlBrowserContainer) {
        int devBrowserTopMargin = dp(72);
        updateFrameLayoutMargins(workspaceBrowserContainer, devBrowserTopMargin, dp(3));
        updateFrameLayoutMargins(controlBrowserContainer, devBrowserTopMargin, dp(3));
    }

    private void updateFrameLayoutMargins(@Nullable View view, int topMargin, int bottomMargin) {
        if (view == null) return;
        ViewGroup.LayoutParams rawLayoutParams = view.getLayoutParams();
        if (!(rawLayoutParams instanceof FrameLayout.LayoutParams)) return;

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) rawLayoutParams;
        if (layoutParams.topMargin == topMargin && layoutParams.bottomMargin == bottomMargin) return;

        layoutParams.topMargin = topMargin;
        layoutParams.bottomMargin = bottomMargin;
        view.setLayoutParams(layoutParams);
    }

    private void applyTerminalSurfaceOffset() {
        DrawerLayout drawerLayout = getDrawer();
        View terminalSurfaceTopInsetView = findViewById(R.id.terminal_surface_top_inset);
        int topInset = ("dev".equals(mCurrentMode) && "terminal".equals(mDevActiveSurface)) ? dp(52) : 0;

        if (drawerLayout != null) {
            drawerLayout.setPadding(0, topInset, 0, 0);
        }

        if (terminalSurfaceTopInsetView != null) {
            ViewGroup.LayoutParams layoutParams = terminalSurfaceTopInsetView.getLayoutParams();
            if (layoutParams.height != topInset) {
                layoutParams.height = topInset;
                terminalSurfaceTopInsetView.setLayoutParams(layoutParams);
            }
            terminalSurfaceTopInsetView.setVisibility(topInset > 0 ? View.VISIBLE : View.GONE);
        }

        if (mTerminalView != null) {
            try {
                mTerminalView.getClass()
                    .getMethod("setTerminalContentTopOffset", Integer.TYPE)
                    .invoke(mTerminalView, topInset);
            } catch (NoSuchMethodException e) {
                mTerminalView.requestLayout();
                mTerminalView.invalidate();
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to apply terminal surface offset", e);
            }
        }
    }

    private void applyTerminalToolbarForMode() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null || mPreferences == null) return;

        setTerminalToolbarHeight();
        boolean visible = mPreferences.shouldShowTerminalToolbar() &&
            (!"dev".equals(mCurrentMode) || "terminal".equals(mDevActiveSurface));

        terminalToolbarViewPager.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setModeSwitcherView() {
        mGlobalModeBarView = findViewById(R.id.global_mode_bar);
        mGlobalModeButtonRowView = findViewById(R.id.global_mode_button_row);
        mGlobalStatusBarView = findViewById(R.id.global_status_bar);
        mGlobalAppTitleView = findViewById(R.id.global_app_title_view);
        if (mGlobalAppTitleView != null && mGlobalAppTitleView.getParent() instanceof View &&
            ((View) mGlobalAppTitleView.getParent()).getParent() instanceof View) {
            mGlobalModeHeaderView = (View) ((View) mGlobalAppTitleView.getParent()).getParent();
        }
        mModeTitleView = findViewById(R.id.mode_title_view);
        mModeSummaryView = findViewById(R.id.mode_summary_view);
        mDailyModeButton = findViewById(R.id.mode_daily_button);
        mDevModeButton = findViewById(R.id.mode_dev_button);
        mAutomationModeButton = findViewById(R.id.mode_automation_button);

        mDailyModeButton.setOnClickListener(v -> switchMode("daily"));
        mDevModeButton.setOnClickListener(v -> switchMode("dev"));
        mAutomationModeButton.setOnClickListener(v -> switchMode("automation"));

        updateModeSwitcherUi();
    }

    private void setDevSurfaceSwitcherView() {
        mDevSurfaceSwitcherView = findViewById(R.id.dev_surface_switcher);
        mDevSurfacePreviewButton = findViewById(R.id.dev_surface_preview_button);
        mDevSurfaceTerminalButton = findViewById(R.id.dev_surface_terminal_button);
        mDevSurfaceControllerButton = findViewById(R.id.dev_surface_controller_button);

        if (mDevSurfacePreviewButton != null) {
            mDevSurfacePreviewButton.setOnClickListener(v -> setActiveDevSurface("preview"));
        }
        if (mDevSurfaceTerminalButton != null) {
            mDevSurfaceTerminalButton.setOnClickListener(v -> setActiveDevSurface("terminal"));
        }
        if (mDevSurfaceControllerButton != null) {
            mDevSurfaceControllerButton.setOnClickListener(v -> setActiveDevSurface("controller"));
        }

        updateDevSurfaceSwitcherUi("dev".equals(mCurrentMode));
    }

    private void setDevTerminalActionsView() {
        bindDevTerminalActionsView(findViewById(android.R.id.content));
        updateDevTerminalActionsUi("dev".equals(mCurrentMode) && "terminal".equals(mDevActiveSurface));
    }

    public void bindDevTerminalActionsView(@Nullable View rootView) {
        View lookupRoot = rootView != null ? rootView : findViewById(android.R.id.content);
        if (lookupRoot == null) return;

        mDevTerminalActionsView = findViewById(R.id.dev_terminal_actions);
        mDevTerminalActionButtonsView = findViewById(R.id.dev_terminal_action_buttons);
        mDevTerminalSessionsButton = findViewById(R.id.dev_terminal_sessions_button);
        mDevTerminalNewSessionButton = findViewById(R.id.dev_terminal_new_session_button);
        mDevTerminalToggleButton = findViewById(R.id.dev_terminal_toggle_button);
        if (mDevTerminalActionsView == null) {
            mDevTerminalActionsView = lookupRoot.findViewById(R.id.dev_terminal_actions);
        }
        if (mDevTerminalActionButtonsView == null) {
            mDevTerminalActionButtonsView = lookupRoot.findViewById(R.id.dev_terminal_action_buttons);
        }
        if (mDevTerminalSessionsButton == null) {
            mDevTerminalSessionsButton = lookupRoot.findViewById(R.id.dev_terminal_sessions_button);
        }
        if (mDevTerminalNewSessionButton == null) {
            mDevTerminalNewSessionButton = lookupRoot.findViewById(R.id.dev_terminal_new_session_button);
        }
        if (mDevTerminalToggleButton == null) {
            mDevTerminalToggleButton = lookupRoot.findViewById(R.id.dev_terminal_toggle_button);
        }

        if (mDevTerminalSessionsButton != null) {
            mDevTerminalSessionsButton.setOnClickListener(v -> {
                DrawerLayout drawer = getDrawer();
                if (drawer != null) {
                    drawer.openDrawer(Gravity.LEFT);
                }
            });
        }

        if (mDevTerminalNewSessionButton != null) {
            mDevTerminalNewSessionButton.setOnClickListener(v ->
                mTermuxTerminalSessionActivityClient.addNewSession(false, null));
            mDevTerminalNewSessionButton.setOnLongClickListener(v -> {
                TextInputDialogUtils.textInput(TermuxActivity.this, R.string.title_create_named_session, null,
                    R.string.action_create_named_session_confirm, text -> mTermuxTerminalSessionActivityClient.addNewSession(false, text),
                    R.string.action_new_session_failsafe, text -> mTermuxTerminalSessionActivityClient.addNewSession(true, text),
                    -1, null, null);
                return true;
            });
        }

        if (mDevTerminalToggleButton != null && mDevTerminalActionsView != null) {
            mDevTerminalToggleButton.setOnClickListener(v ->
                setDevTerminalActionsExpanded(!mDevTerminalActionsExpanded));
            bindDevTerminalActionsDragListener();
            mDevTerminalActionsView.post(this::restoreDevTerminalActionsPosition);
        }

        updateDevTerminalActionsUi("dev".equals(mCurrentMode) && "terminal".equals(mDevActiveSurface));
    }

    private void setActiveDevSurface(@NonNull String surface) {
        if (!"dev".equals(mCurrentMode)) return;
        if (surface.equals(mDevActiveSurface)) return;

        mDevActiveSurface = surface;
        setTerminalToolbarHeight();
        applyModeUi();
    }

    private void updateDevSurfaceSwitcherUi(boolean visible) {
        if (mDevSurfaceSwitcherView != null) {
            mDevSurfaceSwitcherView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        updateDevSurfaceButtonState(mDevSurfacePreviewButton, "preview".equals(mDevActiveSurface));
        updateDevSurfaceButtonState(mDevSurfaceTerminalButton, "terminal".equals(mDevActiveSurface));
        updateDevSurfaceButtonState(mDevSurfaceControllerButton, "controller".equals(mDevActiveSurface));
    }

    private void updateDevTerminalActionsUi(boolean visible) {
        if (mDevTerminalActionsView != null) {
            mDevTerminalActionsView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (mDevTerminalActionButtonsView != null) {
            mDevTerminalActionButtonsView.setVisibility(visible && mDevTerminalActionsExpanded ? View.VISIBLE : View.GONE);
        }
        updateDevTerminalToggleButtonState();
        updateDevTerminalGestureExclusion();
        if (visible && mDevTerminalActionsView != null) {
            mDevTerminalActionsView.post(() -> {
                restoreDevTerminalActionsPosition();
                updateDevTerminalGestureExclusion();
            });
        }

        if (mDevTerminalSessionsButton != null) {
            int sessionCount = 0;
            if (mTermuxService != null) {
                sessionCount = mTermuxService.getTermuxSessions().size();
            }
            mDevTerminalSessionsButton.setText(sessionCount > 0 ?
                getString(R.string.dev_terminal_sessions_count, sessionCount) :
                getString(R.string.dev_terminal_sessions));
        }
    }

    private void setDevTerminalActionsExpanded(boolean expanded) {
        mDevTerminalActionsExpanded = expanded;
        updateDevTerminalActionsUi("dev".equals(mCurrentMode) && "terminal".equals(mDevActiveSurface));
    }

    private void bindDevTerminalActionsDragListener() {
        if (mDevTerminalToggleButton == null || mDevTerminalActionsView == null) return;

        final int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        final int longPressTimeout = ViewConfiguration.getLongPressTimeout();
        mDevTerminalToggleButton.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX;
            private float downRawY;
            private float startX;
            private float startY;
            private long downTime;
            private boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        startX = mDevTerminalActionsView.getX();
                        startY = mDevTerminalActionsView.getY();
                        downTime = event.getEventTime();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - downRawX;
                        float deltaY = event.getRawY() - downRawY;
                        boolean pastLongPress = (event.getEventTime() - downTime) >= longPressTimeout;
                        if (!dragging && pastLongPress &&
                            ((deltaX * deltaX) + (deltaY * deltaY)) > (touchSlop * touchSlop)) {
                            dragging = true;
                        }
                        if (dragging) {
                            moveDevTerminalActionsTo(startX + deltaX, startY + deltaY);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) {
                            v.performClick();
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void restoreDevTerminalActionsPosition() {
        if (mDevTerminalActionsView == null) return;
        if (mDevTerminalActionsPositionInitialized) {
            moveDevTerminalActionsTo(mDevTerminalActionsSavedX, mDevTerminalActionsSavedY);
            return;
        }

        ViewParent parent = mDevTerminalActionsView.getParent();
        if (parent instanceof View) {
            View parentView = (View) parent;
            float defaultX = parentView.getWidth() - mDevTerminalActionsView.getWidth() - dp(32);
            float defaultY = (parentView.getHeight() * 0.55f) - (mDevTerminalActionsView.getHeight() / 2f);
            moveDevTerminalActionsTo(defaultX, defaultY);
            mDevTerminalActionsPositionInitialized = false;
            return;
        }

        mDevTerminalActionsSavedX = mDevTerminalActionsView.getX();
        mDevTerminalActionsSavedY = mDevTerminalActionsView.getY();
    }

    private void moveDevTerminalActionsTo(float desiredX, float desiredY) {
        if (mDevTerminalActionsView == null) return;
        ViewParent parent = mDevTerminalActionsView.getParent();
        if (!(parent instanceof View)) return;

        View parentView = (View) parent;
        float minX = dp(72);
        float minY = dp(20);
        float maxX = Math.max(minX, parentView.getWidth() - mDevTerminalActionsView.getWidth() - dp(72));
        float maxY = Math.max(minY, parentView.getHeight() - mDevTerminalActionsView.getHeight() - dp(20));
        float clampedX = Math.max(minX, Math.min(desiredX, maxX));
        float clampedY = Math.max(minY, Math.min(desiredY, maxY));

        mDevTerminalActionsView.setX(clampedX);
        mDevTerminalActionsView.setY(clampedY);
        mDevTerminalActionsSavedX = clampedX;
        mDevTerminalActionsSavedY = clampedY;
        mDevTerminalActionsPositionInitialized = true;
        updateDevTerminalGestureExclusion();
    }

    private void updateDevTerminalToggleButtonState() {
        if (mDevTerminalToggleButton == null) return;

        mDevTerminalToggleButton.setBackgroundTintList(ColorStateList.valueOf(
            Color.parseColor(mDevTerminalActionsExpanded ? "#2563EB" : "#0F172A")
        ));
        mDevTerminalToggleButton.setTextColor(Color.parseColor(
            mDevTerminalActionsExpanded ? "#FFFFFF" : "#D1D5DB"
        ));
    }

    private void updateDevTerminalGestureExclusion() {
        if (mDevTerminalActionsView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;

        List<Rect> exclusionRects = new ArrayList<>();
        if (mDevTerminalActionsView.getVisibility() == View.VISIBLE) {
            if (mDevTerminalActionButtonsView != null && mDevTerminalActionButtonsView.getVisibility() == View.VISIBLE) {
                exclusionRects.add(new Rect(
                    mDevTerminalActionButtonsView.getLeft(),
                    mDevTerminalActionButtonsView.getTop(),
                    mDevTerminalActionButtonsView.getRight(),
                    mDevTerminalActionButtonsView.getBottom()
                ));
            }
            if (mDevTerminalToggleButton != null && mDevTerminalToggleButton.getVisibility() == View.VISIBLE) {
                exclusionRects.add(new Rect(
                    mDevTerminalToggleButton.getLeft(),
                    mDevTerminalToggleButton.getTop(),
                    mDevTerminalToggleButton.getRight(),
                    mDevTerminalToggleButton.getBottom()
                ));
            }
        }
        mDevTerminalActionsView.setSystemGestureExclusionRects(exclusionRects);
    }

    private void updateDevSurfaceButtonState(@Nullable MaterialButton button, boolean selected) {
        if (button == null) return;

        button.setBackgroundTintList(ColorStateList.valueOf(
            Color.parseColor(selected ? "#2563EB" : "#1F2937")
        ));
        button.setTextColor(Color.parseColor(selected ? "#FFFFFF" : "#D1D5DB"));
    }

    private void switchMode(@NonNull String mode) {
        ModeConfig modeConfig = ModeConfigManager.getModeConfig();
        if (!modeConfig.isModeEnabled(mode)) {
            showToast("Mode disabled: " + mode, true);
            return;
        }
        if (mode.equals(mCurrentMode)) return;

        mCurrentMode = mode;
        setTermuxBrowserView();
        applyModeUi();
    }

    private void updateModeSwitcherUi() {
        if (mModeTitleView == null || mModeSummaryView == null ||
            mDailyModeButton == null || mDevModeButton == null || mAutomationModeButton == null) {
            return;
        }

        ModeConfig modeConfig = ModeConfigManager.getModeConfig();
        mDailyModeButton.setEnabled(modeConfig.isModeEnabled("daily"));
        mDevModeButton.setEnabled(modeConfig.isModeEnabled("dev"));
        mAutomationModeButton.setEnabled(modeConfig.isModeEnabled("automation"));

        mModeTitleView.setText(getModeTitleRes(mCurrentMode));
        mModeSummaryView.setText(getModeSummaryRes(mCurrentMode));

        setModeButtonState(mDailyModeButton, "daily".equals(mCurrentMode));
        setModeButtonState(mDevModeButton, "dev".equals(mCurrentMode));
        setModeButtonState(mAutomationModeButton, "automation".equals(mCurrentMode));
        applyModeSwitcherChrome();
    }

    private void setModeButtonState(@NonNull MaterialButton button, boolean selected) {
        int backgroundColor = Color.parseColor(selected ? "#2563EB" : "#344054");
        int textColor = Color.parseColor(selected ? "#FFFFFF" : "#D0D5DD");
        button.setAlpha(button.isEnabled() ? 1f : 0.45f);
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setTextColor(textColor);
    }

    private void applyModeSwitcherChrome() {
        boolean daily = "daily".equals(mCurrentMode);

        if (mGlobalModeBarView != null) {
            mGlobalModeBarView.setVisibility(daily ? View.GONE : View.VISIBLE);
            mGlobalModeBarView.setPadding(dp(6), dp(4), dp(6), dp(4));
        }

        if (daily) {
            if (mGlobalStatusBarView != null) {
                mGlobalStatusBarView.setVisibility(View.GONE);
            }
            return;
        }

        if (mGlobalModeHeaderView != null) {
            mGlobalModeHeaderView.setVisibility(View.GONE);
        }

        if (mGlobalAppTitleView != null) {
            mGlobalAppTitleView.setVisibility(View.GONE);
        }
        if (mModeTitleView != null) {
            mModeTitleView.setVisibility(View.GONE);
        }
        if (mModeSummaryView != null) {
            mModeSummaryView.setVisibility(View.GONE);
        }

        if (mGlobalModeButtonRowView != null &&
            mGlobalModeButtonRowView.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mGlobalModeButtonRowView.getLayoutParams();
            params.topMargin = 0;
            mGlobalModeButtonRowView.setLayoutParams(params);
        }

        if (mGlobalModeButtonRowView instanceof LinearLayout &&
            ((LinearLayout) mGlobalModeButtonRowView).getChildCount() > 0 &&
            ((LinearLayout) mGlobalModeButtonRowView).getChildAt(0) instanceof LinearLayout) {
            ((LinearLayout) ((LinearLayout) mGlobalModeButtonRowView).getChildAt(0))
                .setGravity(Gravity.CENTER_HORIZONTAL);
        }

        compactModeSwitcherButton(mDailyModeButton);
        compactModeSwitcherButton(mDevModeButton);
        compactModeSwitcherButton(mAutomationModeButton);

        if (mGlobalStatusBarView != null) {
            mGlobalStatusBarView.setVisibility(View.VISIBLE);
        }
    }

    private void compactModeSwitcherButton(@Nullable MaterialButton button) {
        if (button == null) return;

        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setTextSize(10f);
        button.setPadding(0, 0, 0, 0);

        if (button.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
            params.width = dp(84);
            params.weight = 0f;
            params.height = dp(30);
            button.setLayoutParams(params);
        }
    }

    private int getModeTitleRes(@NonNull String mode) {
        switch (mode) {
            case "dev":
                return R.string.mode_title_dev;
            case "automation":
                return R.string.mode_title_automation;
            case "daily":
            default:
                return R.string.mode_title_daily;
        }
    }

    private int getModeSummaryRes(@NonNull String mode) {
        switch (mode) {
            case "dev":
                return R.string.mode_summary_dev;
            case "automation":
                return R.string.mode_summary_automation;
            case "daily":
            default:
                return R.string.mode_summary_daily;
        }
    }

    private int getModeStatusRes(@NonNull String mode) {
        switch (mode) {
            case "dev":
                return R.string.status_right_dev;
            case "automation":
                return R.string.status_right_automation;
            case "daily":
            default:
                return R.string.status_right_daily;
        }
    }

    private TermuxBrowserController buildBrowserController(@NonNull String targetContext,
                                                           @NonNull View panelRoot) {
        TextView browserPanelTitle = panelRoot.findViewById(R.id.browser_panel_title);
        TextView browserStatusView = panelRoot.findViewById(R.id.browser_status_view);

        browserPanelTitle.setText(getString(getBrowserPanelTitleRes(targetContext)));
        browserStatusView.setText(getString(getBrowserPanelStatusRes(targetContext)));

        boolean interactiveChrome = !"workspace_webview".equals(targetContext);
        boolean compactChrome = "dev".equals(mCurrentMode) && "control_webview".equals(targetContext);
        boolean minimalChrome = "daily".equals(mCurrentMode) && "control_webview".equals(targetContext);

        return new TermuxBrowserController(this, mCurrentMode, targetContext, panelRoot, interactiveChrome, compactChrome, minimalChrome);
    }

    public void requestModeSwitch(@NonNull String mode) {
        switchMode(mode);
    }

    private int getBrowserPanelTitleRes(@NonNull String targetContext) {
        if ("workspace_webview".equals(targetContext)) {
            return R.string.browser_panel_title_preview;
        }

        if ("dev".equals(mCurrentMode)) {
            return R.string.browser_panel_title_controller;
        }

        return R.string.browser_panel_title_daily;
    }

    private int getBrowserPanelStatusRes(@NonNull String targetContext) {
        if ("workspace_webview".equals(targetContext)) {
            return R.string.browser_panel_status_dev_preview;
        }

        if ("dev".equals(mCurrentMode)) {
            return R.string.browser_panel_status_dev_controller;
        }

        return R.string.browser_panel_status_daily;
    }

    private void destroyBrowserControllers() {
        if (mControlBrowserController != null) {
            mControlBrowserController.onDestroy();
            mControlBrowserController = null;
        }
        if (mWorkspaceBrowserController != null) {
            mWorkspaceBrowserController.onDestroy();
            mWorkspaceBrowserController = null;
        }
    }

    private void setTermuxSessionsListView() {
        ListView termuxSessionsListView = findViewById(R.id.terminal_sessions_list);
        mTermuxSessionListViewController = new TermuxSessionsListViewController(this, mTermuxService.getTermuxSessions());
        termuxSessionsListView.setAdapter(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemClickListener(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemLongClickListener(mTermuxSessionListViewController);
    }



    private void setTerminalToolbarView(Bundle savedInstanceState) {
        mTermuxTerminalExtraKeys = new TermuxTerminalExtraKeys(this, mTerminalView,
            mTermuxTerminalViewClient, mTermuxTerminalSessionActivityClient);

        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (mPreferences.shouldShowTerminalToolbar()) terminalToolbarViewPager.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        mTerminalToolbarDefaultHeight = layoutParams.height;

        setTerminalToolbarHeight();

        String savedTextInput = null;
        if (savedInstanceState != null)
            savedTextInput = savedInstanceState.getString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT);

        terminalToolbarViewPager.setAdapter(new TerminalToolbarViewPager.PageAdapter(this, savedTextInput));
        terminalToolbarViewPager.addOnPageChangeListener(new TerminalToolbarViewPager.OnPageChangeListener(this, terminalToolbarViewPager));
        applyTerminalToolbarForMode();
    }

    private void setTerminalToolbarHeight() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        layoutParams.height = Math.round(mTerminalToolbarDefaultHeight *
            (mTermuxTerminalExtraKeys.getExtraKeysInfo() == null ? 0 : mTermuxTerminalExtraKeys.getExtraKeysInfo().getMatrix().length) *
            mProperties.getTerminalToolbarHeightScaleFactor());
        terminalToolbarViewPager.setLayoutParams(layoutParams);
    }

    public void toggleTerminalToolbar() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        final boolean showNow = mPreferences.toogleShowTerminalToolbar();
        Logger.showToast(this, (showNow ? getString(R.string.msg_enabling_terminal_toolbar) : getString(R.string.msg_disabling_terminal_toolbar)), true);
        applyTerminalToolbarForMode();
        if (showNow && isTerminalToolbarTextInputViewSelected()) {
            // Focus the text input view if just revealed.
            findViewById(R.id.terminal_toolbar_text_input).requestFocus();
        }
    }

    private void saveTerminalToolbarTextInput(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        final EditText textInputView = findViewById(R.id.terminal_toolbar_text_input);
        if (textInputView != null) {
            String textInput = textInputView.getText().toString();
            if (!textInput.isEmpty()) savedInstanceState.putString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT, textInput);
        }
    }



    private void setSettingsButtonView() {
        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));
        });
    }

    private void setNewSessionButtonView() {
        View newSessionButton = findViewById(R.id.new_session_button);
        newSessionButton.setOnClickListener(v -> mTermuxTerminalSessionActivityClient.addNewSession(false, null));
        newSessionButton.setOnLongClickListener(v -> {
            TextInputDialogUtils.textInput(TermuxActivity.this, R.string.title_create_named_session, null,
                R.string.action_create_named_session_confirm, text -> mTermuxTerminalSessionActivityClient.addNewSession(false, text),
                R.string.action_new_session_failsafe, text -> mTermuxTerminalSessionActivityClient.addNewSession(true, text),
                -1, null, null);
            return true;
        });
    }

    private void setToggleKeyboardView() {
        findViewById(R.id.toggle_keyboard_button).setOnClickListener(v -> {
            mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();
            getDrawer().closeDrawers();
        });

        findViewById(R.id.toggle_keyboard_button).setOnLongClickListener(v -> {
            toggleTerminalToolbar();
            return true;
        });
    }





    @SuppressLint("RtlHardcoded")
    @Override
    public void onBackPressed() {
        if (getDrawer().isDrawerOpen(Gravity.LEFT)) {
            getDrawer().closeDrawers();
        } else if ("dev".equals(mCurrentMode)) {
            if ("preview".equals(mDevActiveSurface) &&
                mWorkspaceBrowserController != null &&
                mWorkspaceBrowserController.handleBackPressed()) {
                return;
            }
            if ("controller".equals(mDevActiveSurface) &&
                mControlBrowserController != null &&
                mControlBrowserController.handleBackPressed()) {
                return;
            }
        } else if (mControlBrowserController != null && mControlBrowserController.handleBackPressed()) {
            return;
        } else {
            finishActivityIfNotFinishing();
        }
    }

    public void finishActivityIfNotFinishing() {
        // prevent duplicate calls to finish() if called from multiple places
        if (!TermuxActivity.this.isFinishing()) {
            finish();
        }
    }

    /** Show a toast and dismiss the last one if still visible. */
    public void showToast(String text, boolean longDuration) {
        if (text == null || text.isEmpty()) return;
        if (mLastToast != null) mLastToast.cancel();
        mLastToast = Toast.makeText(TermuxActivity.this, text, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        mLastToast.setGravity(Gravity.TOP, 0, 0);
        mLastToast.show();
    }



    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        TerminalSession currentSession = getCurrentSession();
        if (currentSession == null) return;

        boolean autoFillEnabled = mTerminalView.isAutoFillEnabled();

        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);
        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);
        if (!DataUtils.isNullOrEmpty(mTerminalView.getStoredSelectedText()))
            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);
        if (autoFillEnabled)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_USERNAME, Menu.NONE, R.string.action_autofill_username);
        if (autoFillEnabled)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_PASSWORD, Menu.NONE, R.string.action_autofill_password);
        menu.add(Menu.NONE, CONTEXT_MENU_RESET_TERMINAL_ID, Menu.NONE, R.string.action_reset_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_KILL_PROCESS_ID, Menu.NONE, getResources().getString(R.string.action_kill_process, getCurrentSession().getPid())).setEnabled(currentSession.isRunning());
        menu.add(Menu.NONE, CONTEXT_MENU_STYLING_ID, Menu.NONE, R.string.action_style_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON, Menu.NONE, R.string.action_toggle_keep_screen_on).setCheckable(true).setChecked(mPreferences.shouldKeepScreenOn());
        menu.add(Menu.NONE, CONTEXT_MENU_HELP_ID, Menu.NONE, R.string.action_open_help);
        menu.add(Menu.NONE, CONTEXT_MENU_SETTINGS_ID, Menu.NONE, R.string.action_open_settings);
        menu.add(Menu.NONE, CONTEXT_MENU_REPORT_ID, Menu.NONE, R.string.action_report_issue);
    }

    /** Hook system menu to show context menu instead. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mTerminalView.showContextMenu();
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = getCurrentSession();

        switch (item.getItemId()) {
            case CONTEXT_MENU_SELECT_URL_ID:
                mTermuxTerminalViewClient.showUrlSelection();
                return true;
            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID:
                mTermuxTerminalViewClient.shareSessionTranscript();
                return true;
            case CONTEXT_MENU_SHARE_SELECTED_TEXT:
                mTermuxTerminalViewClient.shareSelectedText();
                return true;
            case CONTEXT_MENU_AUTOFILL_USERNAME:
                mTerminalView.requestAutoFillUsername();
                return true;
            case CONTEXT_MENU_AUTOFILL_PASSWORD:
                mTerminalView.requestAutoFillPassword();
                return true;
            case CONTEXT_MENU_RESET_TERMINAL_ID:
                onResetTerminalSession(session);
                return true;
            case CONTEXT_MENU_KILL_PROCESS_ID:
                showKillSessionDialog(session);
                return true;
            case CONTEXT_MENU_STYLING_ID:
                showStylingDialog();
                return true;
            case CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON:
                toggleKeepScreenOn();
                return true;
            case CONTEXT_MENU_HELP_ID:
                ActivityUtils.startActivity(this, new Intent(this, HelpActivity.class));
                return true;
            case CONTEXT_MENU_SETTINGS_ID:
                ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));
                return true;
            case CONTEXT_MENU_REPORT_ID:
                mTermuxTerminalViewClient.reportIssueFromTranscript();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        // onContextMenuClosed() is triggered twice if back button is pressed to dismiss instead of tap for some reason
        mTerminalView.onContextMenuClosed(menu);
    }

    private void showKillSessionDialog(TerminalSession session) {
        if (session == null) return;

        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setMessage(R.string.title_confirm_kill_process);
        b.setPositiveButton(android.R.string.yes, (dialog, id) -> {
            dialog.dismiss();
            session.finishIfRunning();
        });
        b.setNegativeButton(android.R.string.no, null);
        b.show();
    }

    private void onResetTerminalSession(TerminalSession session) {
        if (session != null) {
            session.reset();
            showToast(getResources().getString(R.string.msg_terminal_reset), true);

            if (mTermuxTerminalSessionActivityClient != null)
                mTermuxTerminalSessionActivityClient.onResetTerminalSession();
        }
    }

    private void showStylingDialog() {
        Intent stylingIntent = new Intent();
        stylingIntent.setClassName(TermuxConstants.TERMUX_STYLING_PACKAGE_NAME, TermuxConstants.TERMUX_STYLING_APP.TERMUX_STYLING_ACTIVITY_NAME);
        try {
            startActivity(stylingIntent);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            // The startActivity() call is not documented to throw IllegalArgumentException.
            // However, crash reporting shows that it sometimes does, so catch it here.
            new AlertDialog.Builder(this).setMessage(getString(R.string.error_styling_not_installed))
                .setPositiveButton(R.string.action_styling_install,
                    (dialog, which) -> ActivityUtils.startActivity(this, new Intent(Intent.ACTION_VIEW, Uri.parse(TermuxConstants.TERMUX_STYLING_FDROID_PACKAGE_URL))))
                .setNegativeButton(android.R.string.cancel, null).show();
        }
    }
    private void toggleKeepScreenOn() {
        if (mTerminalView.getKeepScreenOn()) {
            mTerminalView.setKeepScreenOn(false);
            mPreferences.setKeepScreenOn(false);
        } else {
            mTerminalView.setKeepScreenOn(true);
            mPreferences.setKeepScreenOn(true);
        }
    }



    /**
     * For processes to access primary external storage (/sdcard, /storage/emulated/0, ~/storage/shared),
     * termux needs to be granted legacy WRITE_EXTERNAL_STORAGE or MANAGE_EXTERNAL_STORAGE permissions
     * if targeting targetSdkVersion 30 (android 11) and running on sdk 30 (android 11) and higher.
     */
    public void requestStoragePermission(boolean isPermissionCallback) {
        new Thread() {
            @Override
            public void run() {
                // Do not ask for permission again
                int requestCode = isPermissionCallback ? -1 : PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION;

                // If permission is granted, then also setup storage symlinks.
                if(PermissionUtils.checkAndRequestLegacyOrManageExternalStoragePermission(
                    TermuxActivity.this, requestCode, !isPermissionCallback)) {
                    if (isPermissionCallback)
                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,
                            getString(com.termux.shared.R.string.msg_storage_permission_granted_on_request));

                    TermuxInstaller.setupStorageSymlinks(TermuxActivity.this);
                } else {
                    if (isPermissionCallback)
                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,
                            getString(com.termux.shared.R.string.msg_storage_permission_not_granted_on_request));
                }
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (mControlBrowserController != null && mControlBrowserController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        if (mWorkspaceBrowserController != null && mWorkspaceBrowserController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
        Logger.logVerbose(LOG_TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: "  + resultCode + ", data: "  + IntentUtils.getIntentString(data));
        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            requestStoragePermission(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Logger.logVerbose(LOG_TAG, "onRequestPermissionsResult: requestCode: " + requestCode + ", permissions: "  + Arrays.toString(permissions) + ", grantResults: "  + Arrays.toString(grantResults));
        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            requestStoragePermission(true);
        }
    }



    public int getNavBarHeight() {
        return mNavBarHeight;
    }

    public TermuxActivityRootView getTermuxActivityRootView() {
        return mTermuxActivityRootView;
    }

    public View getTermuxActivityBottomSpaceView() {
        return mTermuxActivityBottomSpaceView;
    }

    public ExtraKeysView getExtraKeysView() {
        return mExtraKeysView;
    }

    public TermuxTerminalExtraKeys getTermuxTerminalExtraKeys() {
        return mTermuxTerminalExtraKeys;
    }

    public void setExtraKeysView(ExtraKeysView extraKeysView) {
        mExtraKeysView = extraKeysView;
    }

    public DrawerLayout getDrawer() {
        return (DrawerLayout) findViewById(R.id.drawer_layout);
    }


    public ViewPager getTerminalToolbarViewPager() {
        return (ViewPager) findViewById(R.id.terminal_toolbar_view_pager);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public float getTerminalToolbarDefaultHeight() {
        return mTerminalToolbarDefaultHeight;
    }

    public boolean isTerminalViewSelected() {
        return getTerminalToolbarViewPager().getCurrentItem() == 0;
    }

    public boolean isTerminalToolbarTextInputViewSelected() {
        return getTerminalToolbarViewPager().getCurrentItem() == 1;
    }


    public void termuxSessionListNotifyUpdated() {
        mTermuxSessionListViewController.notifyDataSetChanged();
        updateDevTerminalActionsUi("dev".equals(mCurrentMode) && "terminal".equals(mDevActiveSurface));
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public boolean isOnResumeAfterOnCreate() {
        return mIsOnResumeAfterOnCreate;
    }

    public boolean isActivityRecreated() {
        return mIsActivityRecreated;
    }



    public TermuxService getTermuxService() {
        return mTermuxService;
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }

    public TermuxBrowserController getControlBrowserController() {
        return mControlBrowserController;
    }

    public TermuxBrowserController getWorkspaceBrowserController() {
        return mWorkspaceBrowserController;
    }

    public TermuxTerminalViewClient getTermuxTerminalViewClient() {
        return mTermuxTerminalViewClient;
    }

    public TermuxTerminalSessionActivityClient getTermuxTerminalSessionClient() {
        return mTermuxTerminalSessionActivityClient;
    }

    @Nullable
    public TerminalSession getCurrentSession() {
        if (mTerminalView != null)
            return mTerminalView.getCurrentSession();
        else
            return null;
    }

    public TermuxAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxAppSharedProperties getProperties() {
        return mProperties;
    }




    public static void updateTermuxActivityStyling(Context context, boolean recreateActivity) {
        // Make sure that terminal styling is always applied.
        Intent stylingIntent = new Intent(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        stylingIntent.putExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, recreateActivity);
        context.sendBroadcast(stylingIntent);
    }

    private void registerTermuxActivityBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);

        registerReceiver(mTermuxActivityBroadcastReceiver, intentFilter);
    }

    private void unregisterTermuxActivityBroadcastReceiver() {
        unregisterReceiver(mTermuxActivityBroadcastReceiver);
    }

    private void fixTermuxActivityBroadcastReceiverIntent(Intent intent) {
        if (intent == null) return;

        String extraReloadStyle = intent.getStringExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);
        if ("storage".equals(extraReloadStyle)) {
            intent.removeExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);
            intent.setAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);
        }
    }

    class TermuxActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (mIsVisible) {
                fixTermuxActivityBroadcastReceiverIntent(intent);

                switch (intent.getAction()) {
                    case TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH:
                        Logger.logDebug(LOG_TAG, "Received intent to notify app crash");
                        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(context, LOG_TAG);
                        return;
                    case TERMUX_ACTIVITY.ACTION_RELOAD_STYLE:
                        Logger.logDebug(LOG_TAG, "Received intent to reload styling");
                        reloadActivityStyling(intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, true));
                        return;
                    case TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS:
                        Logger.logDebug(LOG_TAG, "Received intent to request storage permissions");
                        requestStoragePermission(false);
                        return;
                    default:
                }
            }
        }
    }

    private void reloadActivityStyling(boolean recreateActivity) {
        if (mProperties != null) {
            reloadProperties();

            if (mExtraKeysView != null) {
                mExtraKeysView.setButtonTextAllCaps(mProperties.shouldExtraKeysTextBeAllCaps());
                mExtraKeysView.reload(mTermuxTerminalExtraKeys.getExtraKeysInfo(), mTerminalToolbarDefaultHeight);
            }

            // Update NightMode.APP_NIGHT_MODE
            TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());
        }

        setMargins();
        setTerminalToolbarHeight();

        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onReloadActivityStyling();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadActivityStyling();

        // To change the activity and drawer theme, activity needs to be recreated.
        // It will destroy the activity, including all stored variables and views, and onCreate()
        // will be called again. Extra keys input text, terminal sessions and transcripts will be preserved.
        if (recreateActivity) {
            Logger.logDebug(LOG_TAG, "Recreating activity");
            TermuxActivity.this.recreate();
        }
    }



    public static void startTermuxActivity(@NonNull final Context context) {
        ActivityUtils.startActivity(context, newInstance(context));
    }

    public static Intent newInstance(@NonNull final Context context) {
        Intent intent = new Intent(context, TermuxActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

}
