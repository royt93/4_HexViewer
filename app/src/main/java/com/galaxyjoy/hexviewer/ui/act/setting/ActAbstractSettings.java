package com.galaxyjoy.hexviewer.ui.act.setting;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.ui.frm.FrmAbstractSettings;

public abstract class ActAbstractSettings extends AppCompatActivity {

    /**
     * Set the base context for this ContextWrapper.
     * All calls will then be delegated to the base context.
     * Throws IllegalStateException if a base context has already been set.
     *
     * @param base The new base context for this wrapper.
     */
//    @Override
//    protected void attachBaseContext(Context base) {
//        super.attachBaseContext(((MyApplication) base.getApplicationContext()).onAttach(base));
//    }

    @Override
    protected void attachBaseContext(Context base) {
        Configuration override = new Configuration(base.getResources().getConfiguration());
        override.fontScale = 1.0f;
        applyOverrideConfiguration(override);
//        super.attachBaseContext(base);
        super.attachBaseContext(((MyApplication) base.getApplicationContext()).onAttach(base));
    }

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

        setContentView(R.layout.act_settings);

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
