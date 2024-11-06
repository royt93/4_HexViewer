package com.galaxyjoy.hexviewer.ui.act.setting;

import android.content.Context;
import android.content.Intent;

import com.galaxyjoy.hexviewer.ui.frm.FrmAbstractSettings;
import com.galaxyjoy.hexviewer.ui.frm.FrmSettingsListsPortraitFrm;

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
  public FrmAbstractSettings onUserCreate() {
    return new FrmSettingsListsPortraitFrm(this);
  }

}