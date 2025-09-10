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
package com.galaxyjoy.hexviewer.ui.act.setting;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;

import com.galaxyjoy.hexviewer.BaseActivity;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.sdkadbmob.UIUtils;
import com.galaxyjoy.hexviewer.ui.frm.FrmAbstractSettings;

public abstract class ActAbstractSettings extends BaseActivity {

    /**
     * User implementation (called in onCreate).
     *
     * @return AbstractSettingsFragment
     */
    public abstract FrmAbstractSettings onUserCreate();

    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.act_settings);
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.layoutRoot));
        //If you want to insert data in your settings
        FrmAbstractSettings prefs = onUserCreate();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingsContainer, prefs)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Called when the options item is clicked (home).
     *
     * @param item The selected menu.
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
