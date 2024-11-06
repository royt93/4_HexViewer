package com.galaxyjoy.hexviewer.ui.activities.settings;

import android.content.Context;
import android.content.Intent;

import com.galaxyjoy.hexviewer.ui.frm.FrmAbstractSettings;
import com.galaxyjoy.hexviewer.ui.frm.SettingsListsLandscapeFrm;

/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Settings activity for lists in landscape mode.
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
public class SettingsListsLandscapeActivity extends AbstractSettingsActivity {

  /**
   * Starts an activity.
   *
   * @param c Android context.
   */
  public static void startActivity(final Context c) {
    Intent intent = new Intent(c, SettingsListsLandscapeActivity.class);
    c.startActivity(intent);
  }

  /**
   * User implementation (called in onCreate).
   *
   * @return AbstractSettingsFragment
   */
  public FrmAbstractSettings onUserCreate() {
    return new SettingsListsLandscapeFrm(this);
  }

}