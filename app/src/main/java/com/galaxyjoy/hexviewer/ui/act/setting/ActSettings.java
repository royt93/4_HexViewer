package com.galaxyjoy.hexviewer.ui.act.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.galaxyjoy.hexviewer.ui.frm.FrmAbstractSettings;
import com.galaxyjoy.hexviewer.ui.frm.FrmSettings;

public class ActSettings extends ActAbstractSettings {
    private static final String ACTIVITY_EXTRA_CHANGE = "ACTIVITY_EXTRA_CHANGE";
    private static final String ACTIVITY_EXTRA_OPEN = "ACTIVITY_EXTRA_OPEN";
    private boolean mChange;
    private boolean mOpen;

    /**
     * Starts an activity.
     *
     * @param c      Android context.
     * @param open   A file is open?
     * @param change A change is detected?
     */
    public static void startActivity(final Context c,
                                     final boolean open,
                                     final boolean change) {
        Intent intent = new Intent(c, ActSettings.class);
        intent.putExtra(ACTIVITY_EXTRA_CHANGE, change);
        intent.putExtra(ACTIVITY_EXTRA_OPEN, open);
        c.startActivity(intent);
    }

    /**
     * User implementation (called in onCreate).
     *
     * @return AbstractSettingsFragment
     */
    public FrmAbstractSettings onUserCreate() {
        mChange = false;
        mOpen = false;
        if (getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            mChange = extras.getBoolean(ACTIVITY_EXTRA_CHANGE);
            mOpen = extras.getBoolean(ACTIVITY_EXTRA_OPEN);
        }

        //If you want to insert data in your settings
        return new FrmSettings(this);
    }

    /**
     * Tests if a change is detected.
     *
     * @return boolean
     */
    public boolean isNotChanged() {
        return !mChange;
    }

    /**
     * Tests if a file is open.
     *
     * @return boolean
     */
    public boolean isOpen() {
        return mOpen;
    }
}
