package com.galaxyjoy.hexviewer.ui.activities.settings;

import android.content.Context;
import android.content.Intent;

import com.galaxyjoy.hexviewer.ui.frm.AbstractSettingsFragment;
import com.galaxyjoy.hexviewer.ui.frm.SettingsFragmentListsPortrait;

/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Settings activity for lists in portrait mode.
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
public class SettingsListsPortraitActivity extends AbstractSettingsActivity {

  /**
   * Starts an activity.
   *
   * @param c Android context.
   */
  public static void startActivity(final Context c) {
    Intent intent = new Intent(c, SettingsListsPortraitActivity.class);
    c.startActivity(intent);
  }

  /**
   * User implementation (called in onCreate).
   *
   * @return AbstractSettingsFragment
   */
  public AbstractSettingsFragment onUserCreate() {
    return new SettingsFragmentListsPortrait(this);
  }

}