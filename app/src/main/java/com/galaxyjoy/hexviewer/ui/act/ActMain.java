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
import com.galaxyjoy.hexviewer.sdkadbmob.AdMobManager;
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
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;

public class ActMain extends ActAbstractBaseMain implements AdapterView.OnItemClickListener, TaskOpen.OpenResultListener, TaskSave.SaveResultListener, AdMobManager.InterstitialAdListener {
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
    //    private MaxAdView adView;
//    private MaxInterstitialAd interstitialAd;
    private AdView adView = null;

    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);
        MyApplication.addLog(this, "Main", "Application started with language: '" + ((MyApplication) getApplicationContext()).getApplicationLanguage(this) + "'");
        setupViews(savedInstanceState);
        AdMobManager.INSTANCE.setCurrentActivity(this);
        AdMobManager.INSTANCE.setInterstitialListener(this);
    }

    @SuppressLint("SetTextI18n")
    private void setupViews(final Bundle savedInstanceState) {
        mApp.setConfiguration(getResources().getConfiguration());
        mUnDoRedo = new UnDoRedo(this);

        mPopup = new MainPopupWindow(this, mUnDoRedo, this::onPopupItemClick);

        LinearLayout mainLayout = findViewById(R.id.mainLayout);
        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("Version " + BuildConfig.VERSION_NAME);

//        LottieAnimationView lottieAnimationView = findViewById(R.id.lottieAnimationView);
//        lottieAnimationView.setAnimation(R.raw.loading);
//        lottieAnimationView.playAnimation();
//        lottieAnimationView.setRepeatCount(LottieDrawable.INFINITE);

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
//        findViewById(R.id.buttonRecentlyOpen).setEnabled(!mApp.getRecentlyOpened().list().isEmpty());
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

        if (savedInstanceState == null) handleIntent(getIntent());

        adView = AdMobManager.INSTANCE.loadBanner(this, BuildConfig.ADMOB_BANNER_ID, findViewById(R.id.flAd), AdSize.BANNER);
        AdMobManager.INSTANCE.loadInterstitial(this, BuildConfig.ADMOB_INTERSTITIAL_ID);
    }

    /**
     * Called when the activity is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
        setRequestedOrientation(mApp.getScreenOrientation(null));
        if (mPopup != null) mPopup.dismiss();
        mApp.applyApplicationLanguage(this);
        /* refresh */
//        findViewById(R.id.buttonRecentlyOpen).setEnabled(!((MyApplication) getApplicationContext()).getRecentlyOpened().list().isEmpty());
        onOpenResult(!FileData.isEmpty(mFileData), false);
        if (mPayloadHexHelper.isVisible()) mPayloadHexHelper.refreshAdapter();
        else if (mPayloadPlainSwipe.isVisible()) mPayloadPlainSwipe.refreshAdapter();
        RoyUtils.rateAppInApp(this, BuildConfig.DEBUG);
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
//        if (adView != null) {
//            ApplovinUtils.destroyAdBanner(findViewById(R.id.flAd), adView);
//        }
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    /**
     * Handles activity intents.
     *
     * @param intent The intent.
     */
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mSearchQuery = intent.getStringExtra(SearchManager.QUERY);
            doSearch(mSearchQuery == null ? "" : mSearchQuery);
        } else {
            if (intent.getData() != null) {
                closeOrphanDialog();
                processIntentUri(getIntent().getData());
            }
        }
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
            } else addRecent = FileHelper.takeUriPermissions(this, uri, false);
            FileData fd = new FileData(this, uri, true);
            mApp.setSequential(fd.getRealSize() != 0);
            final Runnable r = () -> mLauncherOpen.processFileOpen(fd, null, addRecent);
            if (mUnDoRedo.isChanged()) {// a save operation is pending?
                UIHelper.confirmFileChanged(this, mFileData, r, () -> new TaskSave(this, this).execute(new TaskSave.Request(mFileData, mPayloadHexHelper.getAdapter().getEntries().getItems(), r)));
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
        return true;
    }

    /**
     * Should the edit menu in case the file is empty be displayed?
     */
    private void updateEditEmptyMenu() {
        if (mEditEmptyMenu != null) {
            mEditEmptyMenu.setVisible(!FileData.isEmpty(mFileData) && mPayloadHexHelper.getAdapter().getEntries().getItems().isEmpty());
            if (mEditEmptyMenu.isVisible()) {
                mPayloadHexHelper.getAdapter().displayTitle();
            }
        }
    }

    /**
     * This is called for activities that set launchMode to "singleTop" in their package,
     * or if a client used the Intent#FLAG_ACTIVITY_SINGLE_TOP flag when calling startActivity(Intent).
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
        final AdtSearchableListArray laa = ((mPayloadPlainSwipe.isVisible()) ? mPayloadPlainSwipe.getAdapter() : mPayloadHexHelper.getAdapter());
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
            if (mFileData.isOpenFromAppIntent() && mPopup != null) mPopup.setSaveMenuEnable(true);
            mFileData = fd;
            mFileData.clearOpenFromAppIntent();
            refreshTitle();
            mPayloadHexHelper.resetUpdateStatus();
        } else mApp.getRecentlyOpened().remove(fd);
        if (userRunnable != null) userRunnable.run();
    }

    /**
     * Method called when the file is opened.
     *
     * @param success  The result.
     * @param fromOpen Called from open
     */
    @Override
    public void onOpenResult(boolean success, boolean fromOpen) {
        setMenuVisible(mSearchMenu, success);
        boolean checked = mPopup != null && mPopup.getPlainText() != null && mPopup.getPlainText().setEnable(success);
        if (!FileData.isEmpty(mFileData) && mFileData.isOpenFromAppIntent()) {
            if (mPopup != null) mPopup.setSaveMenuEnable(false);
        } else {
            if (mPopup != null) mPopup.setSaveMenuEnable(success);
        }
        if (mPopup != null) {
            mPopup.setMenusEnable(success);
        }
        if (success) {
            mIdleView.setVisibility(View.GONE);
            mPayloadHexHelper.setVisible(!checked);
            mPayloadPlainSwipe.setVisible(checked);
            if (fromOpen) mUnDoRedo.clear();
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
     * Called by the system when the device configuration changes while your activity is running.
     *
     * @param newConfig The new device configuration. This value cannot be null.
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mApp.setConfiguration(newConfig);
        if (mPayloadPlainSwipe.isVisible()) {
            mPayloadPlainSwipe.refresh();
        } else if (mPayloadHexHelper.isVisible()) mPayloadHexHelper.getAdapter().notifyDataSetChanged();
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
//                showAd();
                mLauncherRecentlyOpen.startActivity();
                AdMobManager.INSTANCE.showInterstitial(this);
            }
        } else if (id == R.id.actionSave) {
            popupActionSave();
        } else if (id == R.id.actionSaveAs) {
            popupActionSaveAs();
        } else if (id == R.id.actionClose) {
            popupActionClose();
        } else if (id == R.id.actionSettings) {
//            showAd();
            ActSettings.startActivity(this, !FileData.isEmpty(mFileData), mUnDoRedo.isChanged());
            AdMobManager.INSTANCE.showInterstitial(this);
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
            mLauncherLineUpdate.startActivity(new ByteArrayOutputStream().toByteArray(), 0, 0, mFileData.getShiftOffset(), 0);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has been clicked.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this will be a view provided by the adapter).
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LineEntry e = mPayloadHexHelper.getAdapter().getItem(position);
        if (e == null) return;
        if (mPayloadPlainSwipe.isVisible()) {
            UIHelper.showErrorDialog(this, R.string.error_title, R.string.error_not_supported_in_plain_text);
            return;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (Byte b : e.getRaw())
            byteArrayOutputStream.write(b);
        mLauncherLineUpdate.startActivity(byteArrayOutputStream.toByteArray(), position, 1, mFileData.getShiftOffset(), mPayloadHexHelper.getAdapter().getCurrentLine(position));
    }

    /**
     * Called to handle the click on the back button.
     */
    @Override
    public void onExit() {
        if (mUnDoRedo.isChanged()) {// a save operation is pending?
            Runnable r = this::finish;
            UIHelper.confirmFileChanged(this, mFileData, r, () -> new TaskSave(this, this).execute(new TaskSave.Request(mFileData, mPayloadHexHelper.getAdapter().getEntries().getItems(), r)));
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
            UIHelper.confirmFileChanged(this, mFileData, r, () -> new TaskSave(this, this).execute(new TaskSave.Request(mFileData, mPayloadHexHelper.getAdapter().getEntries().getItems(), r)));
        } else r.run();
    }

    /**
     * Action when the user clicks on the "save" menu.
     */
    private void popupActionSave() {
        if (FileData.isEmpty(mFileData)) {
            UIHelper.showErrorDialog(this, R.string.error_title, getString(R.string.open_a_file_before));
            return;
        }
        new TaskSave(this, this).execute(new TaskSave.Request(mFileData, mPayloadHexHelper.getAdapter().getEntries().getItems(), null));
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
        if (plainText.containsId(id, true)) plainText.toggleCheck();
        boolean checked = plainText.isChecked();
        mPayloadPlainSwipe.setVisible(checked);
        mPayloadHexHelper.setVisible(!checked);
        if (mSearchQuery != null && !mSearchQuery.isEmpty()) doSearch(mSearchQuery);
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
        if (lineNumbers.containsId(id, true)) lineNumbers.toggleCheck();
        boolean checked = lineNumbers.isChecked();
        mApp.setLineNumber(checked);
        if (mPayloadHexHelper.isVisible()) mPayloadHexHelper.refreshLineNumbers();
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
//            findViewById(R.id.buttonRecentlyOpen).setEnabled(!mApp.getRecentlyOpened().list().isEmpty());
        };
        if (mUnDoRedo.isChanged()) {// a save operation is pending?
            UIHelper.confirmFileChanged(this, mFileData, r, () -> new TaskSave(this, this).execute(new TaskSave.Request(mFileData, mPayloadHexHelper.getAdapter().getEntries().getItems(), r)));
        } else r.run();
    }

    /**
     * Action when the user clicks on the "go to xxx" menu.
     */
    private void popupActionGoTo() {
        if (mPopup.getPlainText().isChecked()) setOrphanDialog(mGoToDialog.show(GoToDialog.Mode.LINE_PLAIN));
        else if (mPopup.getLineNumbers().isChecked()) setOrphanDialog(mGoToDialog.show(GoToDialog.Mode.ADDRESS));
        else setOrphanDialog(mGoToDialog.show(GoToDialog.Mode.LINE_HEX));
    }

    @Override
    public void onAdLoaded() {

    }

    @Override
    public void onAdFailedToLoad(@NotNull LoadAdError error) {

    }

    @Override
    public void onAdShowed() {

    }

    @Override
    public void onAdDismissed() {

    }

    @Override
    public void onAdClicked() {

    }

    @Override
    public void onAdFailedToShow(@NotNull AdError error) {

    }

    @Override
    public void onAdNotAvailable() {

    }

//    private void showAd() {
//        boolean enableAdInter = getString(R.string.EnableAdInter).equals("true");
//        if (!enableAdInter) {
//            return;
//        }
//        if (interstitialAd != null && interstitialAd.isReady()) {
//            if (BuildConfig.DEBUG) {
//                Toast.makeText(this, "Show ad FULL SUCCESSFULLY", Toast.LENGTH_SHORT).show();
////                interstitialAd.showAd();
//            } else {
//                interstitialAd.showAd();
//            }
//        }
//    }

//    private void createAdInter() {
//        boolean enableAdInter = getString(R.string.EnableAdInter).equals("true");
//        if (!enableAdInter) {
//            return;
//        }
//        String id = getString(R.string.INTER);
//        if (id.isEmpty()) {
//            return;
//        }
//        interstitialAd = new MaxInterstitialAd(id, this);
//        interstitialAd.setListener(new MaxAdListener() {
//            @Override
//            public void onAdLoaded(@NonNull MaxAd maxAd) {
////                retryAttempt = 0;
//            }
//
//            @Override
//            public void onAdDisplayed(@NonNull MaxAd maxAd) {
//
//            }
//
//            @Override
//            public void onAdHidden(@NonNull MaxAd maxAd) {
//                // Interstitial ad is hidden. Pre-load the next ad
//                interstitialAd.loadAd();
//            }
//
//            @Override
//            public void onAdClicked(@NonNull MaxAd maxAd) {
//
//            }
//
//            @Override
//            public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
////                retryAttempt++;
////                long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, retryAttempt)));
////
////                new Handler().postDelayed(() -> interstitialAd.loadAd(), delayMillis);
//            }
//
//            @Override
//            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {
//                // Interstitial ad failed to display. AppLovin recommends that you load the next ad.
//                interstitialAd.loadAd();
//            }
//        });
//
//        // Load the first ad
//        interstitialAd.loadAd();
//    }

}
