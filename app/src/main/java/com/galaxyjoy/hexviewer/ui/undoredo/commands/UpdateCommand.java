package com.galaxyjoy.hexviewer.ui.undoredo.commands;

import android.util.Log;

import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.activities.MainActivity;
import com.galaxyjoy.hexviewer.ui.adt.HexTextArrayAdt;
import com.galaxyjoy.hexviewer.ui.undoredo.ICommand;
import com.galaxyjoy.hexviewer.ui.undoredo.UnDoRedo;

import java.util.ArrayList;
import java.util.List;

public class UpdateCommand implements ICommand {
    private final List<LineEntry> mList;
    private final MainActivity mActivity;
    private final int mFirstPosition;
    private final int mRefNbLines;
    private final List<LineEntry> mPrevLines;
    private final UnDoRedo mUnDoRedo;


    public UpdateCommand(final UnDoRedo undoRedo,
                         final MainActivity activity,
                         final int firstPosition,
                         final int refNbLines,
                         List<LineEntry> entries) {
        mUnDoRedo = undoRedo;
        mList = entries;
        mActivity = activity;
        mFirstPosition = activity.getPayloadHex().getAdapter().getEntries().getItemIndex(firstPosition);
        mRefNbLines = refNbLines;
        mPrevLines = new ArrayList<>();
    }

    /**
     * Execute the command.
     */
    public void execute() {
        HexTextArrayAdt adapter = mActivity.getPayloadHex().getAdapter();
        String query = mActivity.getSearchQuery();
        if (!query.isEmpty())
            adapter.manualFilterUpdate(""); /* reset filter */

        mPrevLines.clear();
        int size = mList.size();
        /* only existing elements  */
        if (size == mRefNbLines) {
            Log.i(getClass().getName(), "execute -> only existing elements");
            updateExistingElements(adapter);
        } else {
            Log.i(getClass().getName(), "execute -> multiple elements");
            int diff = Math.abs(size - mRefNbLines);

            /* First we modify the existing elements */
            updateExistingElements(adapter);

            /* Then we add the elements */
            addElements(adapter, size);

            /* Finally we move the existing indexes */
            if ((mFirstPosition + size) < adapter.getEntries().getItems().size())
                adapter.getEntries().moveIndexes((mFirstPosition + size), diff, true);
        }
        if (!query.isEmpty())
            adapter.manualFilterUpdate(query); /* restore filter */
        adapter.refresh();
    }

    /**
     * Add the new elements.
     *
     * @param adapter The adapter.
     * @param size    The number of elements
     */
    private void addElements(HexTextArrayAdt adapter, int size) {
        int index = mPrevLines.size() - 1;
        if (index == -1) {
            for (int i = 0; i < size; i++) {
                addValue(adapter, i, i);
            }
        } else {
            LineEntry prevLine = mPrevLines.get(mPrevLines.size() - 1);
            for (int i = mRefNbLines, j = 1; i < size; i++, j++) {
                addValue(adapter, i, prevLine.getIndex() + j);
            }
        }
    }

    /**
     * Adds a value to the adapter.
     *
     * @param adapter    The adapter.
     * @param listIndex  The index in the list to add.
     * @param valueIndex The index of the value (LineEntry::setIndex).
     */
    private void addValue(HexTextArrayAdt adapter,
                          int listIndex,
                          int valueIndex) {
        LineEntry value = mList.get(listIndex);
        value.setIndex(valueIndex);
        value.setUpdated(mUnDoRedo.isChanged());
        if (value.getIndex() < adapter.getEntries().getItems().size()) {
            adapter.getEntries().addItem(value.getIndex(), mList.get(listIndex));
        } else {
            adapter.getEntries().addItem(mList.get(listIndex));
        }
    }

    /**
     * Un-Execute the command.
     */
    public void unExecute() {
        HexTextArrayAdt adapter = mActivity.getPayloadHex().getAdapter();
        String query = mActivity.getSearchQuery();
        if (!query.isEmpty())
            adapter.manualFilterUpdate(""); /* reset filter */

        int size = mList.size();

        /* only existing elements  */
        if (size == mRefNbLines) {
            Log.i(getClass().getName(), "unExecute -> only existing elements");
        } else {
            Log.i(getClass().getName(), "unExecute -> multiple elements");
            int diff = Math.abs(size - mRefNbLines);
            int index = mPrevLines.size() - 1;
            if (index == -1)
                adapter.getEntries().clear();
            else {
                LineEntry prevLine = mPrevLines.get(index);

                /* First, we delete the elements */
                for (int i = (prevLine.getIndex() + diff); i > prevLine.getIndex(); i--) {
                    adapter.getEntries().removeItem(i);
                }

                /* Then we move the existing indexes - filtered */
                adapter.getEntries().moveIndexes(prevLine.getIndex() + 1, diff, false);
            }
        }
        /* Finally we restores the existing elements */
        restoreExistingElements(adapter);

        if (!query.isEmpty())
            adapter.manualFilterUpdate(query); /* restore filter */
        adapter.refresh();
    }

    /**
     * Restores the existing elements.
     *
     * @param adapter HexTextArrayAdapter
     */
    private void restoreExistingElements(HexTextArrayAdt adapter) {
        for (int i = 0; i < mRefNbLines; i++) {
            final LineEntry le = adapter.getItem(mFirstPosition + i);
            final LineEntry oldVal = new LineEntry(mPrevLines.get(i));
            if (!le.toString().equals(oldVal.toString())) {
                oldVal.setUpdated(false);
                le.setValues(oldVal.getPlain(), oldVal.getRaw());
                LineEntry ld = adapter.getItem(le.getIndex());
                ld.setValues(oldVal.getPlain(), oldVal.getRaw());
                ld.setUpdated(oldVal.isUpdated());
            }
        }
    }

    /**
     * Restores the existing elements.
     *
     * @param adapter HexTextArrayAdapter
     */
    private void updateExistingElements(HexTextArrayAdt adapter) {
        for (int i = 0; i < mRefNbLines; i++) {
            final LineEntry le = adapter.getItem(mFirstPosition + i);
            mPrevLines.add(new LineEntry(le));
            final LineEntry newVal = new LineEntry(mList.get(i));
            if (!le.toString().equals(newVal.toString())) {
                newVal.setUpdated(mUnDoRedo.isChanged());
                le.setValues(newVal.getPlain(), newVal.getRaw());
                LineEntry ld = adapter.getItem(le.getIndex());
                ld.setValues(le.getPlain(), le.getRaw());
                ld.setUpdated(newVal.isUpdated());
            }
        }
    }
}
