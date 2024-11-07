package com.galaxyjoy.hexviewer.ui.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.act.ActLineUpdate;
import com.galaxyjoy.hexviewer.ui.act.MainAct;
import com.galaxyjoy.hexviewer.ui.adt.AdtHexTextArray;
import com.galaxyjoy.hexviewer.util.SysHelper;
import com.galaxyjoy.hexviewer.MyApplication;

public class LauncherLineUpdate {
    private final MainAct mActivity;
    private final MyApplication mApp;
    private ActivityResultLauncher<Intent> activityResultLauncherLineUpdate;

    public LauncherLineUpdate(MainAct activity) {
        mActivity = activity;
        mApp = (MyApplication) mActivity.getApplicationContext();
        register();
    }

    /**
     * Starts the activity.
     *
     * @param texts    The hex texts.
     * @param position The position in the list view.
     */
    public void startActivity(final byte[] texts,
                              final int position,
                              final int nbLines,
                              final int shiftOffset,
                              final long startRow) {
        Intent intent = new Intent(mActivity, ActLineUpdate.class);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_TEXTS, texts);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_POSITION, position);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_NB_LINES, nbLines);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_FILENAME, mActivity.getFileData().getName());
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_CHANGE, mActivity.getUnDoRedo().isChanged());
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_SEQUENTIAL, mActivity.getFileData().isSequential());
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_SHIFT_OFFSET, shiftOffset);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_START_OFFSET, startRow);
        activityResultLauncherLineUpdate.launch(intent);
    }

    private void processIntentData(Intent data) {
        Bundle bundle = data.getExtras();
        String refString = bundle.getString(ActLineUpdate.RESULT_REFERENCE_STRING);
        String newString = bundle.getString(ActLineUpdate.RESULT_NEW_STRING);
        int position = bundle.getInt(ActLineUpdate.RESULT_POSITION);
        int nbLines = bundle.getInt(ActLineUpdate.RESULT_NB_LINES);

        byte[] buf = SysHelper.hexStringToByteArray(newString);
        final byte[] ref = SysHelper.hexStringToByteArray(refString);
        if (Arrays.equals(ref, buf)) {
            /* nothing to do */
            return;
        }

        final List<LineEntry> li = new ArrayList<>();
        int nbBytesPerLine = mApp.getNbBytesPerLine();
        if (position == 0 && mActivity.getFileData().getShiftOffset() != 0) {
            byte[] b = new byte[Math.min(buf.length, nbBytesPerLine - mActivity.getFileData().getShiftOffset())];
            System.arraycopy(buf, 0, b, 0, b.length);
            SysHelper.formatBuffer(li, b, b.length, null, nbBytesPerLine, mActivity.getFileData().getShiftOffset());
            byte[] buff2 = new byte[buf.length - b.length];
            System.arraycopy(buf,
                    b.length,
                    buff2,
                    0,
                    buff2.length);
            buf = buff2;
        }
        processUndoRedo(li,
                buf,
                nbBytesPerLine,
                position,
                nbLines);
    }

    private void processUndoRedo(final List<LineEntry> li,
                                 final byte[] buf,
                                 final int nbBytesPerLine,
                                 final int position,
                                 final int nbLines) {
        SysHelper.formatBuffer(li, buf, buf.length, null, nbBytesPerLine);
        AdtHexTextArray adapter = mActivity.getPayloadHex().getAdapter();
        if (li.isEmpty()) {
            Map<Integer, LineEntry> map = new HashMap<>();
            for (int i = position; i < position + nbLines; i++) {
                map.put(adapter.getEntries().getItemIndex(i), adapter.getItem(i));
            }
            mActivity.getUnDoRedo().insertInUnDoRedoForDelete(mActivity, map).execute();
        } else if (li.size() >= nbLines) {
            mActivity.getUnDoRedo().insertInUnDoRedoForUpdate(mActivity, position, nbLines, li).execute();
        } else {
            Map<Integer, LineEntry> map = new HashMap<>();
            for (int i = position + li.size(); i < position + nbLines; i++) {
                map.put(adapter.getEntries().getItemIndex(i), adapter.getItem(i));
            }
            mActivity.getUnDoRedo().insertInUnDoRedoForUpdateAndDelete(mActivity, position, li, map).execute();
        }
    }

    /**
     * Registers result launcher for the activity for line update.
     */
    private void register() {
        activityResultLauncherLineUpdate = mActivity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            processIntentData(data);
                        } else {
                            Log.e(getClass().getSimpleName(), "Null data!!!");
                            MyApplication.addLog(mActivity, "LineUpdate", "Null intent data!");
                        }
                    }
                });
    }

}
