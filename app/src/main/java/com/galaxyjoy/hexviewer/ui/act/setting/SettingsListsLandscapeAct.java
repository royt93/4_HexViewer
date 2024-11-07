package com.galaxyjoy.hexviewer.ui.act.setting;

import android.content.Context;
import android.content.Intent;

import com.galaxyjoy.hexviewer.ui.frm.FrmAbstractSettings;
import com.galaxyjoy.hexviewer.ui.frm.FrmSettingsListsLandscape;

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
public class SettingsListsLandscapeAct extends ActAbstractSettings {

  /**
   * Starts an activity.
   *
   * @param c Android context.
   */
  public static void startActivity(final Context c) {
    Intent intent = new Intent(c, SettingsListsLandscapeAct.class);
    c.startActivity(intent);
  }

  /**
   * User implementation (called in onCreate).
   *
   * @return AbstractSettingsFragment
   */
  public FrmAbstractSettings onUserCreate() {
    return new FrmSettingsListsLandscape(this);
  }

}