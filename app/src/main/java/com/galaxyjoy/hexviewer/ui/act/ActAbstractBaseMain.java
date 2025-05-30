package com.galaxyjoy.hexviewer.ui.act;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.galaxyjoy.hexviewer.BaseActivity;
import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.ui.util.UIHelper;
import com.galaxyjoy.hexviewer.util.SysHelper;

public abstract class ActAbstractBaseMain extends BaseActivity {
    private static final int BACK_TIME_DELAY = 2000;
    private long mLastBackPressed = -1;
    private SearchView mSearchView = null;
    private AlertDialog mOrphanDialog = null;
    protected MyApplication mApp = null;

    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp = (MyApplication) getApplicationContext();

        setupViews();
    }

    private void setupViews() {
        /* sanity check */
        String[] languages = getResources().getStringArray(R.array.languages_values);
        boolean found = false;
        for (String language : languages)
            if (language.equals(mApp.getApplicationLanguage(this))) {
                found = true;
                break;
            }
        if (!found) {
            mApp.setApplicationLanguage("en-US");
            recreate();
        }

        /* permissions */
        boolean requestPermissions = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        if (requestPermissions)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                back();
            }
        });
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
    }

    protected void setSearchView(MenuItem si) {
        // Searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            mSearchView = (SearchView) si.getActionView();
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            mSearchView.setTextDirection(SysHelper.isRTL(this) ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
//            mSearchView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            mSearchView.setInputType(InputType.TYPE_CLASS_TEXT);
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    doSearch(s);
                    return true;
                }
            });
        }
    }

    /**
     * Sets the visibility of the menu item.
     *
     * @param menu    MenuItem
     * @param visible If true then the item will be visible; if false it is hidden.
     */
    protected void setMenuVisible(final MenuItem menu, final boolean visible) {
        if (menu != null) menu.setVisible(visible);
    }

    /**
     * Sets the orphan dialog.
     *
     * @param orphan The dialog.
     */
    public void setOrphanDialog(AlertDialog orphan) {
        if (mOrphanDialog != null && mOrphanDialog.isShowing()) {
            mOrphanDialog.dismiss();
        }
        mOrphanDialog = orphan;
    }

    protected void closeOrphanDialog() {
        if (mOrphanDialog != null) {
            if (mOrphanDialog.isShowing()) mOrphanDialog.dismiss();
            mOrphanDialog = null;
        }
    }

    /**
     * Performs the research.
     *
     * @param queryStr The query string.
     */
    public abstract void doSearch(String queryStr);

    /**
     * Cancels search.
     */
    protected void cancelSearch() {
        if (mSearchView != null && !mSearchView.isIconified()) {
            doSearch("");
            mSearchView.setIconified(true);
        }
    }

    /**
     * Called to handle the exit of the application.
     */
    protected abstract void onExit();

    /**
     * Called to handle the click on the back button.
     */
    private void back() {
        if (mSearchView != null && !mSearchView.isIconified()) {
            cancelSearch();
        } else {
            if (mLastBackPressed + BACK_TIME_DELAY > System.currentTimeMillis()) {
                onExit();
                return;
            } else {
                UIHelper.toast(this, getString(R.string.on_double_back_exit_text));
            }
            mLastBackPressed = System.currentTimeMillis();
        }
    }
}
