/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.act;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MenuCompat;

import com.galaxyjoy.hexviewer.BuildConfig;
import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.ext.RoyUtils;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.roy.sdkadbmob.AdManager;
import com.google.android.gms.ads.AdSize;
import com.roy.sdkadbmob.UIUtils;
import com.galaxyjoy.hexviewer.ui.act.setting.ActSettings;
import com.galaxyjoy.hexviewer.ui.adt.AdtSearchableListArray;
import com.galaxyjoy.hexviewer.ui.dlg.GoToDialog;
import com.galaxyjoy.hexviewer.ui.launcher.LauncherLineUpdate;
import com.galaxyjoy.hexviewer.ui.launcher.LauncherOpen;
import com.galaxyjoy.hexviewer.ui.launcher.LauncherPartialOpen;
import com.galaxyjoy.hexviewer.ui.launcher.LauncherRecentlyOpen;
import com.galaxyjoy.hexviewer.ui.launcher.LauncherSave;
import com.galaxyjoy.hexviewer.ui.payload.PayloadHexHelper;
import com.galaxyjoy.hexviewer.ui.payload.PayloadPlainSwipe;
import com.galaxyjoy.hexviewer.ui.popup.MainPopupWindow;
import com.galaxyjoy.hexviewer.ui.popup.PopupCheckboxHelper;
import com.galaxyjoy.hexviewer.ui.task.TaskOpen;
import com.galaxyjoy.hexviewer.ui.task.TaskSave;
import com.galaxyjoy.hexviewer.ui.undoredo.UnDoRedo;
import com.galaxyjoy.hexviewer.ui.util.UIHelper;
import com.galaxyjoy.hexviewer.util.io.FileHelper;
import com.galaxyjoy.hexviewer.util.WebViewOomFix;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ActMain extends ActAbstractBaseMain implements AdapterView.OnItemClickListener,
        TaskOpen.OpenResultListener, TaskSave.SaveResultListener {
    private FileData mFileData = null;
    private ConstraintLayout mIdleView = null;
    private MenuItem mSearchMenu = null;
    private MenuItem mEditEmptyMenu = null;
    private String mSearchQuery = "";
    private PayloadPlainSwipe mPayloadPlainSwipe = null;
    private LauncherLineUpdate mLauncherLineUpdate = null;
    private LauncherSave mLauncherSave = null;
    private LauncherOpen mLauncherOpen = null;
    private LauncherRecentlyOpen mLauncherRecentlyOpen = null;
    private LauncherPartialOpen mLauncherPartialOpen;
    private UnDoRedo mUnDoRedo = null;
    private MainPopupWindow mPopup = null;
    private PayloadHexHelper mPayloadHexHelper = null;
    private GoToDialog mGoToDialog = null;
    private View adView = null;

    private android.view.View mVipBadgeContainer = null;
    private android.widget.ImageView mImgVipIcon = null;
    private android.widget.TextView mTvVipLabel = null;
    private android.animation.ObjectAnimator mPillAnimator = null;


    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.act_main);
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.layoutRoot), true, true);
        MyApplication.addLog(this, "Main", "Application started with language: '"
                + ((MyApplication) getApplicationContext()).getApplicationLanguage(this) + "'");
        setupViews(savedInstanceState);
    }

    @SuppressLint("SetTextI18n")
    private void setupViews(final Bundle savedInstanceState) {
        mApp.setConfiguration(getResources().getConfiguration());
        mUnDoRedo = new UnDoRedo(this);

        mPopup = new MainPopupWindow(this, mUnDoRedo, this::onPopupItemClick);

        LinearLayout mainLayout = findViewById(R.id.mainLayout);
        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("Version " + BuildConfig.VERSION_NAME);

        mIdleView = findViewById(R.id.idleView);
        mIdleView.setVisibility(View.VISIBLE);

        findViewById(R.id.buttonOpenFile).setOnClickListener(v -> {
            onPopupItemClick(R.id.actionOpen);
        });
        findViewById(R.id.buttonPartialOpenFile).setOnClickListener(v -> {
            onPopupItemClick(R.id.actionOpenSequential);
        });
        findViewById(R.id.buttonRecentlyOpen).setOnClickListener(v -> {
            onPopupItemClick(R.id.actionRecentlyOpen);
        });
        findViewById(R.id.buttonHash).setOnClickListener(v -> {
            AdManager.INSTANCE.showInterstitial(this, adShown -> {
                // Guard: SDK may fire this callback while the activity is being destroyed
                // (e.g. during a recreate), when launchers/navigation are no longer valid.
                if (isFinishing() || isDestroyed()) return null;
                startActivity(new Intent(this, ActHashCalculator.class));
                return null;
            });
        });
        findViewById(R.id.buttonFileInfo).setOnClickListener(v -> {
            AdManager.INSTANCE.showInterstitial(this, adShown -> {
                if (isFinishing() || isDestroyed()) return null;
                startActivity(new Intent(this, ActFileInfo.class));
                return null;
            });
        });
        // findViewById(R.id.buttonRecentlyOpen).setEnabled(!mApp.getRecentlyOpened().list().isEmpty());
        mPayloadHexHelper = new PayloadHexHelper();
        mPayloadHexHelper.onCreate(this);

        mPayloadPlainSwipe = new PayloadPlainSwipe();
        mPayloadPlainSwipe.onCreate(this);

        mLauncherOpen = new LauncherOpen(this, mainLayout);
        mLauncherSave = new LauncherSave(this);
        mLauncherLineUpdate = new LauncherLineUpdate(this);
        mLauncherRecentlyOpen = new LauncherRecentlyOpen(this);
        mLauncherPartialOpen = new LauncherPartialOpen(this);

        mGoToDialog = new GoToDialog(this);

        if (savedInstanceState == null)
            handleIntent(getIntent());

        refreshBannerState();
        AdManager.INSTANCE.loadInterstitial(this);
    }

    /**
     * Called when the activity is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        MyApplication.addLog(this, "ActMain", "onResume | file=" + (mFileData == null ? "null" : mFileData.getName())
                + " | isChanged=" + (mUnDoRedo != null && mUnDoRedo.isChanged())
                + " | hexVisible=" + (mPayloadHexHelper != null && mPayloadHexHelper.isVisible())
                + " | plainVisible=" + (mPayloadPlainSwipe != null && mPayloadPlainSwipe.isVisible()));
        refreshBannerState();
        updateVipBadge(mVipBadgeContainer, mImgVipIcon, mTvVipLabel);
        startPillAnimation(mVipBadgeContainer);
        setRequestedOrientation(mApp.getScreenOrientation(null));
        if (mPopup != null)
            mPopup.dismiss();
        mApp.applyApplicationLanguage(this);
        /* refresh */
        // findViewById(R.id.buttonRecentlyOpen).setEnabled(!((MyApplication)
        // getApplicationContext()).getRecentlyOpened().list().isEmpty());
        onOpenResult(!FileData.isEmpty(mFileData), false);
        if (mPayloadHexHelper.isVisible())
            mPayloadHexHelper.refreshAdapter();
        else if (mPayloadPlainSwipe.isVisible())
            mPayloadPlainSwipe.refreshAdapter();
        RoyUtils.rateAppInApp(this, BuildConfig.DEBUG);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        MyApplication.addLog(this, "ActMain", "onSaveInstanceState | file=" + (mFileData == null ? "null" : mFileData.getName())
                + " | isChanged=" + (mUnDoRedo != null && mUnDoRedo.isChanged()));
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        MyApplication.addLog(this, "ActMain", "onRestoreInstanceState called");
    }

    @Override
    protected void onPause() {
        MyApplication.addLog(this, "ActMain", "onPause | file=" + (mFileData == null ? "null" : mFileData.getName())
                + " | isChanged=" + (mUnDoRedo != null && mUnDoRedo.isChanged()));
        stopPillAnimation();
        // FIX: Pause AdView (WebView-based) rendering timers to stop the draw loop
        // that triggers setRequestedFrameRate → Debug.getCallers → OOM on Android 14+
        if (adView instanceof android.webkit.WebView) {
            WebViewOomFix.pauseWebViewTimers((android.webkit.WebView) adView);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopPillAnimation();
        if (adView != null) {
            // FIX: Restore default frame rate before destroying to avoid
            // any pending setRequestedFrameRate calls during teardown
            WebViewOomFix.restoreWebViewFrameRate(adView);
            AdManager.INSTANCE.bannerDestroy(adView);
            adView = null;
        }
        if (mPayloadPlainSwipe != null) {
            mPayloadPlainSwipe.onDestroy();
        }
        if (mPayloadHexHelper != null) {
            mPayloadHexHelper.onDestroy();
        }
        if (mGoToDialog != null) {
            mGoToDialog.cleanup();
        }
        if (mUnDoRedo != null) {
            mUnDoRedo.cleanup();
        }
        if (mPopup != null) {
            mPopup.dismiss();
        }
        super.onDestroy();
    }

    /**
     * Handles activity intents.
     *
     * @param intent The intent.
     */
    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        // Validate intent action - only allow known safe actions
        String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            mSearchQuery = intent.getStringExtra(SearchManager.QUERY);
            doSearch(mSearchQuery == null ? "" : mSearchQuery);
        } else if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) {
            // Security: Validate intent for VIEW/EDIT actions
            if (!validateIntent(intent)) {
                UIHelper.showErrorDialog(this, getString(R.string.error_title),
                        "Invalid or unsafe file source. Please use the Open File menu instead.");
                return;
            }

            if (intent.getData() != null) {
                closeOrphanDialog();
                processIntentUri(intent.getData());
            }
        } else if (action == null && intent.getData() != null) {
            // Handle intents without explicit action but with data
            if (!validateIntent(intent)) {
                UIHelper.showErrorDialog(this, getString(R.string.error_title),
                        "Invalid or unsafe file source. Please use the Open File menu instead.");
                return;
            }
            closeOrphanDialog();
            processIntentUri(intent.getData());
        }
    }

    /**
     * Validates intent for security - prevents malicious file opening attacks.
     *
     * @param intent The intent to validate
     * @return true if intent is safe to process
     */
    private boolean validateIntent(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return false;
        }

        Uri uri = intent.getData();
        String scheme = uri.getScheme();

        // Only allow content:// and file:// schemes - block others like javascript://,
        // data://
        if (scheme == null || (!scheme.equals("content") && !scheme.equals("file"))) {
            android.util.Log.w("ActMain", "Rejected intent with unsafe scheme: " + scheme);
            return false;
        }

        // Validate mime type if available
        String mimeType = intent.getType();
        if (mimeType != null && !isAllowedMimeType(mimeType)) {
            android.util.Log.w("ActMain", "Rejected intent with unsafe mime type: " + mimeType);
            return false;
        }

        // Additional validation: Check file size if possible
        try {
            long fileSize = com.galaxyjoy.hexviewer.util.io.FileHelper.getFileSize(this, getContentResolver(), uri);
            if (fileSize > com.galaxyjoy.hexviewer.constants.AppConstants.MAX_EXTERNAL_INTENT_FILE_SIZE) {
                String maxSizeStr = com.galaxyjoy.hexviewer.util.SysHelper.sizeToHuman(this,
                        com.galaxyjoy.hexviewer.constants.AppConstants.MAX_EXTERNAL_INTENT_FILE_SIZE,
                        true, true, false);
                UIHelper.showErrorDialog(this, getString(R.string.error_title),
                        "File too large (max " + maxSizeStr
                                + " for external files). Use Open File menu for larger files.");
                return false;
            }
        } catch (Exception e) {
            // If we can't get file size, allow but log
            android.util.Log.w("ActMain", "Could not validate file size: " + e.getMessage());
        }

        return true;
    }

    /**
     * Checks if mime type is in allowed list.
     *
     * @param mimeType MIME type to check
     * @return true if allowed
     */
    private boolean isAllowedMimeType(String mimeType) {
        if (mimeType == null) {
            return true; // Allow null mime type
        }

        // Whitelist of allowed mime types matching our intent filters
        return mimeType.startsWith("text/") ||
                mimeType.startsWith("image/") ||
                mimeType.equals("application/octet-stream") ||
                mimeType.equals("application/x-binary") ||
                mimeType.equals("application/zip") ||
                mimeType.equals("application/pdf");
    }

    /**
     * Processes the intent Uri.
     *
     * @param uri Uri
     */
    private void processIntentUri(final Uri uri) {
        if (uri != null) {
            boolean addRecent;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                addRecent = false;
            } else
                addRecent = FileHelper.takeUriPermissions(this, uri, false);
            FileData fd = new FileData(this, uri, true);
            mApp.setSequential(fd.getRealSize() != 0);
            final Runnable r = () -> mLauncherOpen.processFileOpen(fd, null, addRecent);
            if (mUnDoRedo.isChanged()) {// a save operation is pending?
                UIHelper.confirmFileChanged(this, mFileData, r, () -> new TaskSave(this, this).execute(
                        new TaskSave.Request(mFileData, mPayloadHexHelper.getAdapter().getEntries().getItems(), r)));
            } else {
                r.run();
            }
        }
    }

    /**
     * Called to create the option menu.
     *
     * @param menu The main menu.
     * @return boolean
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        mEditEmptyMenu = menu.findItem(R.id.menuActionEditEmpty);
        mEditEmptyMenu.setVisible(!(mFileData == null || mFileData.getRealSize() != 0));
        mSearchMenu = menu.findItem(R.id.menuActionSearch);
        mSearchMenu.setVisible(false);
        setSearchView(mSearchMenu);

        MenuItem vipItem = menu.findItem(R.id.menuActionVip);
        if (vipItem != null && vipItem.getActionView() != null) {
            View actionView = vipItem.getActionView();
            mVipBadgeContainer = actionView.findViewById(R.id.layoutVipBadgeContainer);
            mImgVipIcon = actionView.findViewById(R.id.imgVipIcon);
            mTvVipLabel = actionView.findViewById(R.id.tvVipLabel);

            View.OnClickListener vipClick = v -> openVipScreen();
            if (mVipBadgeContainer != null) mVipBadgeContainer.setOnClickListener(vipClick);

            updateVipBadge(mVipBadgeContainer, mImgVipIcon, mTvVipLabel);
            startPillAnimation(mVipBadgeContainer);
        }
        return true;
    }

    /**
     * Should the edit menu in case the file is empty be displayed?
     */
    private void updateEditEmptyMenu() {
        if (mEditEmptyMenu != null) {
            mEditEmptyMenu.setVisible(
                    !FileData.isEmpty(mFileData) && mPayloadHexHelper.getAdapter().getEntries().getItems().isEmpty());
            if (mEditEmptyMenu.isVisible()) {
                mPayloadHexHelper.getAdapter().displayTitle();
            }
        }
    }

    /**
     * This is called for activities that set launchMode to "singleTop" in their
     * package,
     * or if a client used the Intent#FLAG_ACTIVITY_SINGLE_TOP flag when calling
     * startActivity(Intent).
     *
     * @param intent The new intent that was started for the activity.
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Performs the research.
     *
     * @param queryStr The query string.
     */
    @Override
    public void doSearch(String queryStr) {
        mSearchQuery = queryStr;
        final AdtSearchableListArray laa = ((mPayloadPlainSwipe.isVisible()) ? mPayloadPlainSwipe.getAdapter()
                : mPayloadHexHelper.getAdapter());
        laa.getFilter().filter(queryStr);
    }

    /**
     * Method called when the file is saved.
     *
     * @param fd           The new FileData.
     * @param success      The result.
     * @param userRunnable User runnable (can be null).
     */
    @Override
    public void onSaveResult(FileData fd, boolean success, final Runnable userRunnable) {
        if (success) {
            mUnDoRedo.refreshChange();
            if (mFileData.isOpenFromAppIntent() && mPopup != null)
                mPopup.setSaveMenuEnable(true);
            mFileData = fd;
            mFileData.clearOpenFromAppIntent();
            refreshTitle();
            mPayloadHexHelper.resetUpdateStatus();
        } else
            mApp.getRecentlyOpened().remove(fd);
        if (userRunnable != null)
            userRunnable.run();
    }

    /**
     * Method called when the file is opened.
     *
     * @param success  The result.
     * @param fromOpen Called from open
     */
    @Override
    public void onOpenResult(boolean success, boolean fromOpen) {
        MyApplication.addLog(this, "ActMain", "onOpenResult | success=" + success + " | fromOpen=" + fromOpen
                + " | file=" + (mFileData == null ? "null" : mFileData.getName()));
        setMenuVisible(mSearchMenu, success);
        boolean checked = mPopup != null && mPopup.getPlainText() != null && mPopup.getPlainText().setEnable(success);
        if (!FileData.isEmpty(mFileData) && mFileData.isOpenFromAppIntent()) {
            if (mPopup != null)
                mPopup.setSaveMenuEnable(false);
        } else {
            if (mPopup != null)
                mPopup.setSaveMenuEnable(success);
        }
        if (mPopup != null) {
            mPopup.setMenusEnable(success);
        }
        if (success) {
            mIdleView.setVisibility(View.GONE);
            mPayloadHexHelper.setVisible(!checked);
            mPayloadPlainSwipe.setVisible(checked);
            if (fromOpen)
                mUnDoRedo.clear();
        } else {
            mIdleView.setVisibility(View.VISIBLE);
            mPayloadHexHelper.setVisible(false);
            mPayloadPlainSwipe.setVisible(false);
            mFileData = null;
            mUnDoRedo.clear();
        }
        updateEditEmptyMenu();
        refreshTitle();
    }

    /**
     * Refreshes the activity title.
     */
    public void refreshTitle() {
        UIHelper.setTitle(this, FileData.isEmpty(mFileData) ? null : mFileData.getName(), mUnDoRedo.isChanged());
        if ((!FileData.isEmpty(mFileData) && !mFileData.isOpenFromAppIntent()))
            mPopup.setSaveMenuEnable(mUnDoRedo.isChanged());
        updateEditEmptyMenu();
    }

    /**
     * Called by the system when the device configuration changes while your
     * activity is running.
     *
     * @param newConfig The new device configuration. This value cannot be null.
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mApp.setConfiguration(newConfig);
        if (mPayloadPlainSwipe.isVisible()) {
            mPayloadPlainSwipe.refresh();
        } else if (mPayloadHexHelper.isVisible())
            mPayloadHexHelper.getAdapter().notifyDataSetChanged();
        // Checks the orientation of the screen
        if (!FileData.isEmpty(mFileData)) {
            refreshTitle();
        }
    }

    /**
     * Handles the click on the popup menu item.
     *
     * @param id The view id.
     */
    public void onPopupItemClick(int id) {
        if (id == R.id.actionOpen || id == R.id.actionOpenSequential) {
            popupActionOpen(id == R.id.actionOpenSequential);
        } else if (id == R.id.actionRecentlyOpen) {
            if (mApp.getRecentlyOpened().list().isEmpty()) {
                UIHelper.toast(this, getString(R.string.no_data_available));
            } else {
                AdManager.INSTANCE.showInterstitial(this, adShown -> {
                    // Guard: SDK may fire this callback during activity destroy/recreate.
                    // Launching via an ActivityResultLauncher then crashes with
                    // "unregistered ActivityResultLauncher".
                    if (isFinishing() || isDestroyed()) return null;
                    mLauncherRecentlyOpen.startActivity();
                    return null;
                });
            }
        } else if (id == R.id.actionSave) {
            popupActionSave();
        } else if (id == R.id.actionSaveAs) {
            popupActionSaveAs();
        } else if (id == R.id.actionClose) {
            popupActionClose();
        } else if (id == R.id.actionSettings) {
            AdManager.INSTANCE.showInterstitial(this, adShown -> {
                if (isFinishing() || isDestroyed()) return null;
                ActSettings.startActivity(ActMain.this, !FileData.isEmpty(mFileData), mUnDoRedo.isChanged());
                return null;
            });
        } else if (id == R.id.actionUndo) {
            mUnDoRedo.undo();
        } else if (id == R.id.actionRedo) {
            mUnDoRedo.redo();
        } else if (id == R.id.actionGoTo) {
            popupActionGoTo();
        } else if (id == R.id.actionRate) {
            RoyUtils.rateApp(this, this.getPackageName());
        } else if (id == R.id.actionMoreApp) {
            RoyUtils.getMoreApps(this);
        } else if (id == R.id.actionShareApp) {
            RoyUtils.shareApp(this, this.getPackageName());
        } else if (id == R.id.actionAbout) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.actionGithubOriginal) {
            RoyUtils.openBrowser(this, "https://github.com/Keidan/HexViewer");
        } else if (id == R.id.actionGithubFork) {
            RoyUtils.openBrowser(this, "https://github.com/royt93/4_HexViewer");
        } else if (mPopup != null) {
            specialPopupActions(id);
        }
    }

    /**
     * Handles the click on the popup menu item.
     *
     * @param id The view id.
     */
    private void specialPopupActions(int id) {
        if (mPopup.getPlainText() != null && mPopup.getPlainText().containsId(id, false)) {
            popupActionPlainText(id, mPopup.getPlainText(), mPopup.getLineNumbers());
        } else if (mPopup.getLineNumbers() != null && mPopup.getLineNumbers().containsId(id, false)) {
            popupActionLineNumbers(id, mPopup.getLineNumbers());
        }
    }

    /**
     * Called when the user select an option menu item.
     *
     * @param item The selected item.
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menuActionMore) {
            mPopup.show(findViewById(R.id.menuActionMore));
        } else if (id == R.id.menuActionEditEmpty) {
            mLauncherLineUpdate.startActivity(new ByteArrayOutputStream().toByteArray(), 0, 0,
                    mFileData.getShiftOffset(), 0);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has been
     * clicked.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this will
     *                 be a view provided by the adapter).
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LineEntry e = mPayloadHexHelper.getAdapter().getItem(position);
        if (e == null)
            return;
        if (mPayloadPlainSwipe.isVisible()) {
            UIHelper.showErrorDialog(this, R.string.error_title, R.string.error_not_supported_in_plain_text);
            return;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (Byte b : e.getRaw())
            byteArrayOutputStream.write(b);
        mLauncherLineUpdate.startActivity(byteArrayOutputStream.toByteArray(), position, 1, mFileData.getShiftOffset(),
                mPayloadHexHelper.getAdapter().getCurrentLine(position));
    }

    /**
     * Called to handle the click on the back button.
     */
    @Override
    public void onExit() {
        if (mUnDoRedo.isChanged()) {// a save operation is pending?
            Runnable r = this::finish;
            UIHelper.confirmFileChanged(this, mFileData, r, () -> new TaskSave(this, this).execute(
                    new TaskSave.Request(mFileData, mPayloadHexHelper.getAdapter().getEntries().getItems(), r)));
        } else {
            finish();
        }
    }

    /* ------------ EXPORTED METHODS ------------ */

    /**
     * Returns the launcher used with the partial open
     *
     * @return LauncherPartialOpen
     */
    public LauncherPartialOpen getLauncherPartialOpen() {
        return mLauncherPartialOpen;
    }

    /**
     * Returns the menu RecentlyOpen
     *
     * @return MenuItem
     */
    public TextView getMenuRecentlyOpen() {
        return mPopup == null ? null : mPopup.getMenuRecentlyOpen();
    }

    private void openVipScreen() {
        Intent intent = new Intent(this, com.galaxyjoy.hexviewer.feature.vip.ActVipManagement.class);
        startActivity(intent);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        } else {
            overridePendingTransition(0, 0);
        }
    }

    private void updateVipBadge(android.view.View container, android.widget.ImageView icon, android.widget.TextView label) {
        if (container == null || icon == null || label == null) return;
        boolean active = AdManager.INSTANCE.isVipByKeyActive();
        if (active) {
            container.setBackgroundResource(R.drawable.bg_pill_active);
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK));
            label.setText(R.string.vip_badge_text);
            label.setTextColor(android.graphics.Color.BLACK);
        } else {
            container.setBackgroundResource(R.drawable.bg_pill_free);
            int color = android.graphics.Color.WHITE;
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(color));
            label.setText(R.string.vip_badge_get_vip);
            label.setTextColor(color);
        }
    }

    private void startPillAnimation(android.view.View pillView) {
        if (pillView == null) return;
        stopPillAnimation();
        mPillAnimator = android.animation.ObjectAnimator.ofPropertyValuesHolder(
            pillView,
            android.animation.PropertyValuesHolder.ofFloat(android.view.View.SCALE_X, 1.0f, 1.06f),
            android.animation.PropertyValuesHolder.ofFloat(android.view.View.SCALE_Y, 1.0f, 1.06f)
        );
        mPillAnimator.setDuration(1300L);
        mPillAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        mPillAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        mPillAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        mPillAnimator.start();
    }

    private void stopPillAnimation() {
        if (mPillAnimator != null) {
            mPillAnimator.cancel();
            mPillAnimator = null;
        }
        if (mVipBadgeContainer != null) {
            mVipBadgeContainer.setScaleX(1.0f);
            mVipBadgeContainer.setScaleY(1.0f);
        }
    }

    private void refreshBannerState() {
        boolean isVip = AdManager.INSTANCE.isVipByKeyActive();
        View adContainer = findViewById(R.id.layoutAdBanner);
        if (adContainer != null) {
            adContainer.setVisibility(isVip ? View.GONE : View.VISIBLE);
        }
        if (isVip) {
            if (adView != null) {
                // FIX: Restore frame rate before destroying the ad view
                WebViewOomFix.restoreWebViewFrameRate(adView);
                AdManager.INSTANCE.bannerDestroy(adView);
                adView = null;
            }
        } else {
            if (adView == null) {
                adView = AdManager.INSTANCE.loadBanner(
                        this,
                        (android.view.ViewGroup) findViewById(R.id.bannerContainer),
                        (android.widget.TextView) findViewById(R.id.tvLabelAd),
                        AdManager.INSTANCE.getAdaptiveBannerSize(this),
                        true
                );
                // FIX: Throttle AdView (WebView-based) frame-rate to 30fps on Android 14+.
                // This halves the frequency of Debug.getCallers() allocations inside
                // setRequestedFrameRate(), preventing OOM under low-heap conditions.
                if (adView != null) {
                    WebViewOomFix.throttleWebViewFrameRate(adView);
                    // Use hardware layer to offload rendering from the CPU
                    WebViewOomFix.setHardwareLayer(adView, true);
                }
            }
        }
    }

    /**
     * Returns the file data.
     *
     * @return FileData
     */
    public FileData getFileData() {
        return mFileData;
    }

    /**
     * Sets the file data.
     *
     * @param fd FileData
     */
    public void setFileData(FileData fd) {
        mFileData = fd;
    }

    /**
     * Returns the search query.
     *
     * @return String
     */
    public String getSearchQuery() {
        return mSearchQuery;
    }

    /**
     * Returns the PayloadHexHelper
     *
     * @return PayloadHexHelper
     */
    public PayloadHexHelper getPayloadHex() {
        return mPayloadHexHelper;
    }

    /**
     * Returns the PayloadPlainSwipe
     *
     * @return PayloadPlainSwipe
     */
    public PayloadPlainSwipe getPayloadPlain() {
        return mPayloadPlainSwipe;
    }

    /**
     * Returns the LauncherOpen
     *
     * @return LauncherOpen
     */
    public LauncherOpen getLauncherOpen() {
        return mLauncherOpen;
    }

    /**
     * Returns the LauncherLineUpdate
     *
     * @return LauncherLineUpdate
     */
    public LauncherLineUpdate getLauncherLineUpdate() {
        return mLauncherLineUpdate;
    }

    /**
     * Returns the undo/redo.
     *
     * @return UnDoRedo
     */
    public UnDoRedo getUnDoRedo() {
        return mUnDoRedo;
    }

    /* ------------ POPUP ACTIONS ------------ */

    /**
     * Action when the user clicks on the "open" or "sequential opening" menu.
     */
    private void popupActionOpen(boolean sequential) {
        mApp.setSequential(sequential);
        final Runnable r = () -> {
            mLauncherOpen.startActivity();
            onOpenResult(false, false);
        };
        if (mUnDoRedo.isChanged()) {// a save operation is pending?
            UIHelper.confirmFileChanged(this, mFileData, r, () -> new TaskSave(this, this).execute(
                    new TaskSave.Request(mFileData, mPayloadHexHelper.getAdapter().getEntries().getItems(), r)));
        } else
            r.run();
    }

    /**
     * Action when the user clicks on the "save" menu.
     */
    private void popupActionSave() {
        if (FileData.isEmpty(mFileData)) {
            UIHelper.showErrorDialog(this, R.string.error_title, getString(R.string.open_a_file_before));
            return;
        }
        new TaskSave(this, this)
                .execute(new TaskSave.Request(mFileData, mPayloadHexHelper.getAdapter().getEntries().getItems(), null));
        refreshTitle();
    }

    /**
     * Action when the user clicks on the "save as" menu.
     */
    private void popupActionSaveAs() {
        if (FileData.isEmpty(mFileData)) {
            UIHelper.showErrorDialog(this, R.string.error_title, getString(R.string.open_a_file_before));
            return;
        }
        mLauncherSave.startActivity();
    }

    /**
     * Action when the user clicks on the "plain text" menu.
     *
     * @param id          Action id.
     * @param plainText   Plain text checkbox.
     * @param lineNumbers Line numbers checkbox.
     */
    private void popupActionPlainText(int id, PopupCheckboxHelper plainText, PopupCheckboxHelper lineNumbers) {
        if (plainText.containsId(id, true))
            plainText.toggleCheck();
        boolean checked = plainText.isChecked();
        mPayloadPlainSwipe.setVisible(checked);
        mPayloadHexHelper.setVisible(!checked);
        if (mSearchQuery != null && !mSearchQuery.isEmpty())
            doSearch(mSearchQuery);
        refreshLineNumbers(lineNumbers);
    }

    /**
     * Refreshes the lines number
     *
     * @param lineNumbers Line numbers checkbox.
     */
    private void refreshLineNumbers(PopupCheckboxHelper lineNumbers) {
        if (lineNumbers != null) {
            boolean checked = lineNumbers.isChecked();
            if (mPayloadHexHelper.isVisible()) {
                if (mApp.isLineNumber() && !checked) {
                    lineNumbers.setChecked(true);
                    mPayloadHexHelper.refreshLineNumbers();
                }
                lineNumbers.setEnable(true);
            } else if (mPayloadPlainSwipe.isVisible()) {
                if (checked) {
                    lineNumbers.setChecked(false);
                    mPayloadHexHelper.refreshLineNumbers();
                }
                lineNumbers.setEnable(false);
            }
            mPopup.refreshGoToName();
        }
    }

    /**
     * Action when the user clicks on the "line numbers" menu.
     *
     * @param id          Action id.
     * @param lineNumbers Line numbers checkbox.
     */
    private void popupActionLineNumbers(int id, PopupCheckboxHelper lineNumbers) {
        if (lineNumbers.containsId(id, true))
            lineNumbers.toggleCheck();
        boolean checked = lineNumbers.isChecked();
        mApp.setLineNumber(checked);
        if (mPayloadHexHelper.isVisible())
            mPayloadHexHelper.refreshLineNumbers();
        mPopup.refreshGoToName();
    }

    /**
     * Action when the user clicks on the "close" menu.
     */
    private void popupActionClose() {
        final Runnable r = () -> {
            onOpenResult(false, false);
            mPayloadPlainSwipe.getAdapter().clear();
            mPayloadHexHelper.getAdapter().clear();
            cancelSearch();
            // findViewById(R.id.buttonRecentlyOpen).setEnabled(!mApp.getRecentlyOpened().list().isEmpty());
        };
        if (mUnDoRedo.isChanged()) {// a save operation is pending?
            UIHelper.confirmFileChanged(this, mFileData, r, () -> new TaskSave(this, this).execute(
                    new TaskSave.Request(mFileData, mPayloadHexHelper.getAdapter().getEntries().getItems(), r)));
        } else
            r.run();
    }

    /**
     * Action when the user clicks on the "go to xxx" menu.
     */
    private void popupActionGoTo() {
        if (mPopup.getPlainText().isChecked())
            setOrphanDialog(mGoToDialog.show(GoToDialog.Mode.LINE_PLAIN));
        else if (mPopup.getLineNumbers().isChecked())
            setOrphanDialog(mGoToDialog.show(GoToDialog.Mode.ADDRESS));
        else
            setOrphanDialog(mGoToDialog.show(GoToDialog.Mode.LINE_HEX));
    }

    /**
     * Called when the OS requests memory trimming.
     *
     * OOM root cause: MediaTek BoostFwk (FrameIdentify) runs on the main thread
     * and fails to allocate an ArrayList during vsync because the heap is exhausted.
     * The adapter holds the entire List<LineEntry> in memory — this is the largest
     * single consumer of heap. Clearing it under pressure prevents the system OOM.
     *
     * Strategy:
     * - TRIM_MEMORY_RUNNING_CRITICAL / COMPLETE: clear adapters immediately
     * - Lower levels: let the Application-level handler (shrink log buffer) handle it
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        MyApplication.addLog(this, "ActMain", "onTrimMemory | level=" + level
                + " | TRIM_MEMORY_UI_HIDDEN=20 | TRIM_MEMORY_COMPLETE=80"
                + " | file=" + (mFileData == null ? "null" : mFileData.getName()));
        // BUG FIX: dùng >= TRIM_MEMORY_COMPLETE (80) thay vì >= TRIM_MEMORY_RUNNING_CRITICAL (15).
        // TRIM_MEMORY_UI_HIDDEN = 20 luôn được gọi khi app vào background bình thường (>= 15),
        // khiến mFileData bị xóa dù không hết memory → mất state chỉnh sửa.
        if (level >= TRIM_MEMORY_COMPLETE) {
            // Heap gần cạn kiệt — giải phóng dữ liệu adapter ngay lập tức
            // để main thread có đủ memory cho MediaTek BoostFwk hoạt động
            if (mPayloadHexHelper != null && mPayloadHexHelper.getAdapter() != null) {
                mPayloadHexHelper.getAdapter().clear();
            }
            if (mPayloadPlainSwipe != null && mPayloadPlainSwipe.getAdapter() != null) {
                mPayloadPlainSwipe.getAdapter().clear();
            }
            mFileData = null;
            UIHelper.setTitle(this, null, false);
            UIHelper.toast(this, getString(R.string.not_enough_memory));
            // FIX: Trim AdView (WebView) caches to reclaim heap space
            // This directly addresses OOM caused by setRequestedFrameRate on Android 14+
            if (adView instanceof android.webkit.WebView) {
                WebViewOomFix.trimWebViewMemory((android.webkit.WebView) adView);
            }
            System.gc();
        }
    }

}

