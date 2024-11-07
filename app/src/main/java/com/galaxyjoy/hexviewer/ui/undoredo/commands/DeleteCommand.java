package com.galaxyjoy.hexviewer.ui.undoredo.commands;

import android.util.Log;

import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.act.ActMain;
import com.galaxyjoy.hexviewer.ui.adt.AdtHexTextArray;
import com.galaxyjoy.hexviewer.ui.undoredo.ICommand;
import com.galaxyjoy.hexviewer.util.SysHelper;

import java.util.List;
import java.util.Map;

public class DeleteCommand implements ICommand {
    private final Map<Integer, LineEntry> mList;
    private final ActMain mActivity;

    public DeleteCommand(final ActMain activity,
                         final Map<Integer, LineEntry> entries) {
        mList = entries;
        mActivity = activity;
    }

    /**
     * Execute the command.
     */
    public void execute() {
        Log.i(getClass().getName(), "execute");
        AdtHexTextArray adapter = mActivity.getPayloadHex().getAdapter();
        String query = mActivity.getSearchQuery();
        if (!query.isEmpty())
            adapter.manualFilterUpdate(""); /* reset filter */
        List<Integer> list = SysHelper.getMapKeys(mList);

        for (int i = list.size() - 1; i >= 0; i--) {
            int position = list.get(i);
            adapter.getEntries().removeItem(position);
        }
        /* rebuilds origin indexes */
        adapter.getEntries().reloadAllIndexes(0);
        if (!query.isEmpty())
            adapter.manualFilterUpdate(query); /* restore filter */
        adapter.refresh();
    }

    /**
     * Un-Execute the command.
     */
    public void unExecute() {
        Log.i(getClass().getName(), "unExecute");
        AdtHexTextArray adapter = mActivity.getPayloadHex().getAdapter();
        String query = mActivity.getSearchQuery();
        if (!query.isEmpty())
            adapter.manualFilterUpdate(""); /* reset filter */

        for (Integer i : SysHelper.getMapKeys(mList)) {
            LineEntry ld = mList.get(i);
            if (ld != null) {
                adapter.getEntries().addItem(ld.getIndex(), ld);
            }
        }
        /* rebuilds origin indexes */
        adapter.getEntries().reloadAllIndexes(0);

        if (!query.isEmpty())
            adapter.manualFilterUpdate(query); /* restore filter */
        adapter.refresh();
    }
}
