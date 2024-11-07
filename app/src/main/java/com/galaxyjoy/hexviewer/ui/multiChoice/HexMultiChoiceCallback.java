package com.galaxyjoy.hexviewer.ui.multiChoice;

import android.annotation.SuppressLint;
import android.view.ActionMode;
import android.view.MenuItem;
import android.widget.ListView;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.act.ActMain;
import com.galaxyjoy.hexviewer.ui.adt.AdtHexTextArray;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HexMultiChoiceCallback extends GenericMultiChoiceCallback {

    @SuppressLint("InflateParams")
    public HexMultiChoiceCallback(ActMain activityMain,
                                  final ListView listView,
                                  final AdtHexTextArray adapter) {
        super(activityMain, listView, adapter);
    }

    /**
     * Returns the menu id.
     *
     * @return R.menu.main_plain_multi_choice
     */
    public int getMenuId() {
        return R.menu.menu_main_hex_multi_choice;
    }

    /**
     * Copy action.
     *
     * @param mode The ActionMode providing the selection mode.
     * @return false on error.
     */
    protected boolean actionCopy(ActionMode mode) {
        List<Integer> selected = new ArrayList<>(mAdapter.getSelectedIds());
        if (selected.isEmpty()) {
            displayError(R.string.error_no_line_selected);
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for (Integer i : selected) {
            LineEntry ld = mAdapter.getItem(i);
            sb.append(ld.getPlain()).append("\n");
        }
        return copyAndClose("CopyHex", mode, sb);
    }

    /**
     * Clear action.
     *
     * @param item The item that was clicked.
     * @param mode The ActionMode providing the selection mode.
     */
    protected void actionClear(MenuItem item, ActionMode mode) {
        setActionView(item, () -> {
            final List<Integer> selected = mAdapter.getSelectedIds();
            Map<Integer, LineEntry> map = new HashMap<>();
            // Captures all selected ids with a loop
            for (int i = selected.size() - 1; i >= 0; i--) {
                int position = selected.get(i);
                LineEntry lf = mAdapter.getItem(position);
                map.put(position, lf);
            }
            mActivity.getUnDoRedo().insertInUnDoRedoForDelete(mActivity, map).execute();
            mActivity.refreshTitle();
            closeActionMode(mode, false);
        });
    }

    /**
     * Edit action.
     *
     * @param mode The ActionMode providing the selection mode.
     * @return false on error.
     */
    protected boolean actionEdit(ActionMode mode) {
        if (!mActivity.getSearchQuery().trim().isEmpty()) {
            displayError(R.string.error_edition_search_in_progress);
            return false;
        }
        List<Integer> selected = new ArrayList<>(mAdapter.getSelectedIds());
        if (selected.isEmpty()) {
            displayError(R.string.error_no_line_selected);
            return false;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int previous = selected.get(0);
        for (Integer i : selected) {
            if (previous != i && previous + 1 != i) {
                displayError(R.string.error_edition_continuous_selection);
                return false;
            }
            previous = i;
            LineEntry ld = mAdapter.getItem(i);
            for (Byte b : ld.getRaw())
                byteArrayOutputStream.write(b);
        }
        mActivity.getLauncherLineUpdate().startActivity(byteArrayOutputStream.toByteArray(),
                selected.get(0), selected.size(),
                mActivity.getFileData().getShiftOffset(), ((AdtHexTextArray) mAdapter).getCurrentLine(selected.get(0)));
        closeActionMode(mode, true);
        return true;
    }

}
