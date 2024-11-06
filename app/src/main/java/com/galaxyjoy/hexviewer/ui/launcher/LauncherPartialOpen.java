package com.galaxyjoy.hexviewer.ui.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.ui.activities.MainActivity;
import com.galaxyjoy.hexviewer.ui.activities.PartialOpenActivity;
import com.galaxyjoy.hexviewer.ui.task.TaskOpen;
import com.galaxyjoy.hexviewer.MyApplication;

/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Launcher used with the partial open part.
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
public class LauncherPartialOpen {
  private final MainActivity mActivity;
  private FileData mPrevious;
  private boolean mAddRecent;
  private String mOldToString;
  private ActivityResultLauncher<Intent> activityResultLauncherOpen;
  private final MyApplication mApp;

  public LauncherPartialOpen(MainActivity activity) {
    mActivity = activity;
    mApp = (MyApplication) activity.getApplicationContext();
    register();
  }

  /**
   * Starts the activity.
   */
  public void startActivity(final FileData previous, final String oldToString, final boolean addRecent) {
    mPrevious = previous;
    mAddRecent = addRecent;
    mOldToString = oldToString;
    PartialOpenActivity.startActivity(mActivity, activityResultLauncherOpen, mActivity.getFileData());
  }

  /**
   * Registers result launcher for the activity for opening a file.
   */
  private void register() {
    Runnable cancel = () -> {
      mApp.setSequential(false);
      if (mPrevious == null) {
        mActivity.onOpenResult(false, false);
      } else {
        mActivity.setFileData(mPrevious);
        mActivity.refreshTitle();
      }
    };
    activityResultLauncherOpen = mActivity.registerForActivityResult(
      new ActivityResultContracts.StartActivityForResult(),
      result -> {
        mApp.setSequential(false);
        if (result.getResultCode() == Activity.RESULT_OK) {
          Intent data = result.getData();
          if (data != null) {
            Bundle bundle = data.getExtras();
            final long startOffset = bundle.getLong(PartialOpenActivity.RESULT_START_OFFSET);
            final long endOffset = bundle.getLong(PartialOpenActivity.RESULT_END_OFFSET);
            mActivity.getFileData().setOffsets(startOffset, endOffset, endOffset != 0L);
            mActivity.getUnDoRedo().clear();
            new TaskOpen(mActivity, mActivity.getPayloadHex().getAdapter(), mActivity, mOldToString, mAddRecent).execute(mActivity.getFileData());
          } else {
            Log.e(getClass().getSimpleName(), "LauncherPartialOpen -> Invalid data object!!!");
            MyApplication.addLog(mActivity, "PartialOpen", "Null intent data!");
            cancel.run();
          }
        } else {
          cancel.run();
        }
      });
  }
}
