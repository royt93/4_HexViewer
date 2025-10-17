/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * </p>
 *
 * @author Keidan
 * <p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.multiChoice;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.ui.act.ActMain;
import com.galaxyjoy.hexviewer.ui.adt.AdtSearchableListArray;
import com.galaxyjoy.hexviewer.ui.util.UIHelper;
import com.galaxyjoy.hexviewer.util.SysHelper;

public abstract class GenericMultiChoiceCallback implements AbsListView.MultiChoiceModeListener {
    private final ListView mListView;
    protected final AdtSearchableListArray mAdapter;
    protected final ActMain mActivity;
    private final ClipboardManager mClipboard;
    private int mFirstSelection = -1;
    private final AlertDialog mProgress;
    private MenuItem mMenuItemSelectAll;
    private final Handler mActionHandler;
    private boolean mIsSelectingAll = false;
    private ActionMode mCurrentActionMode = null;
    private int mBatchUpdateCounter = 0;

    // Performance optimization constants
    private static final int SMALL_FILE_THRESHOLD = 500;
    private static final int MEDIUM_FILE_THRESHOLD = 5000;
    private static final int LARGE_FILE_THRESHOLD = 20000;
    private static final int TITLE_UPDATE_INTERVAL = 10; // Update title every N batches

    @SuppressLint("InflateParams")
    protected GenericMultiChoiceCallback(ActMain activityMain, final ListView listView, final AdtSearchableListArray adapter) {
        mActivity = activityMain;
        mListView = listView;
        mAdapter = adapter;
        mClipboard = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        mProgress = UIHelper.createCircularProgressDialog(mActivity, null);
        mActionHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Returns the menu id.
     *
     * @return R.menu.xxx
     */
    public abstract int getMenuId();

    /**
     * Called when action mode is first created. The menu supplied will be used to generate action buttons for the action mode.
     *
     * @param mode ActionMode being created.
     * @param menu Menu used to populate action buttons.
     * @return true if the action mode should be created, false if entering this mode should be aborted.
     */
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mCurrentActionMode = mode;
        mode.getMenuInflater().inflate(getMenuId(), menu);
        mMenuItemSelectAll = menu.findItem(R.id.menuActionSelectAll);
        return true;
    }

    /**
     * Called to refresh an action mode's action menu whenever it is invalidated.
     *
     * @param mode ActionMode being prepared.
     * @param menu Menu used to populate action buttons.
     * @return true if the menu or action mode was updated, false otherwise.
     */
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /**
     * Called to report a user click on an action button.
     *
     * @param mode The current ActionMode.
     * @param item The item that was clicked.
     * @return true if this callback handled the event, false if the standard MenuItem invocation should continue.
     */
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.menuActionClear) {
            if (mActivity.getFileData().isSequential()) {
                UIHelper.showErrorDialog(mActivity, mActivity.getFileData().getName(),
                        mActivity.getString(R.string.error_open_sequential_add_or_delete_data));
                return false;
            }
            actionClear(item, mode);
            return true;
        } else if (item.getItemId() == R.id.menuActionSelectAll) {
            actionSelectAll(item);
            return true;
        } else if (item.getItemId() == R.id.menuActionEdit) {
            return actionEdit(mode);
        } else if (item.getItemId() == R.id.menuActionCopy) {
            return actionCopy(mode);
        }
        return false;
    }

    /**
     * Called when an action mode is about to be exited and destroyed.
     *
     * @param mode The current ActionMode being destroyed.
     */
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mIsSelectingAll = false;
        mCurrentActionMode = null;
        mBatchUpdateCounter = 0;
        mAdapter.removeSelection();
        if (mProgress.isShowing())
            mProgress.dismiss();
        if (mActionHandler != null) {
            mActionHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Called when an item is checked or unchecked during selection mode.
     *
     * @param mode     The ActionMode providing the selection mode.
     * @param position Adapter position of the item that was checked or unchecked.
     * @param id       Adapter ID of the item that was checked or unchecked.
     * @param checked  true if the item is now checked, false if the item is now unchecked.
     */
    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mAdapter.toggleSelection(position, checked);

        // Skip UI updates during batch selection to prevent OOM
        if (mIsSelectingAll) {
            // Batch update title periodically instead of every item
            mBatchUpdateCounter++;
            if (mBatchUpdateCounter % TITLE_UPDATE_INTERVAL == 0) {
                updateActionModeTitle(mode);
            }
            return;
        }

        // Normal single-item selection
        final int checkedCount = mListView.getCheckedItemCount();
        mode.setTitle(String.format(mActivity.getString(R.string.items_selected), checkedCount));
        if (checkedCount == 1)
            mFirstSelection = mAdapter.getSelectedIds().get(0);
        if (mMenuItemSelectAll != null)
            mMenuItemSelectAll.setChecked(!mMenuItemSelectAll.isChecked() &&
                    mAdapter.getSelectedCount() == mAdapter.getCount());
    }

    /**
     * Update action mode title - extracted for reuse
     */
    private void updateActionModeTitle(ActionMode mode) {
        if (mode != null) {
            final int checkedCount = mListView.getCheckedItemCount();
            mode.setTitle(String.format(mActivity.getString(R.string.items_selected), checkedCount));
        }
    }

    /**
     * Select all action.
     *
     * @param item The item that was clicked.
     */
    private void actionSelectAll(MenuItem item) {
        final int count = mAdapter.getCount();
        final boolean checked = mAdapter.getSelectedCount() != mAdapter.getCount();

        // Warn user for extremely large selections
        if (count > LARGE_FILE_THRESHOLD) {
            UIHelper.showErrorDialog(mActivity,
                mActivity.getString(R.string.error_title),
                "Cannot select more than " + LARGE_FILE_THRESHOLD + " items at once. Current count: " + count);
            return;
        }

        // Dynamic batch size based on file size for optimal performance
        final int batchSize = calculateOptimalBatchSize(count);
        final long batchDelay = calculateOptimalDelay(count);

        // Reset counter for batch updates
        mBatchUpdateCounter = 0;

        // Small files: process immediately without progress dialog
        if (count <= SMALL_FILE_THRESHOLD) {
            mIsSelectingAll = true;
            processSmallFileDirectly(count, checked, item);
            return;
        }

        // Medium/Large files: use batching with progress dialog
        UIHelper.showCircularProgressDialog(mProgress);

        mActionHandler.postDelayed(() -> {
            mIsSelectingAll = true;
            processBatch(0, count, batchSize, batchDelay, checked, item);
        }, 50); // Reduced initial delay from 100ms to 50ms
    }

    /**
     * Calculate optimal batch size based on total item count
     * Larger batches for smaller files = faster processing
     */
    private int calculateOptimalBatchSize(int totalCount) {
        if (totalCount <= SMALL_FILE_THRESHOLD) {
            return totalCount; // Process all at once
        } else if (totalCount <= MEDIUM_FILE_THRESHOLD) {
            return 200; // Medium batches
        } else {
            return 150; // Smaller batches for large files
        }
    }

    /**
     * Calculate optimal delay between batches
     * Shorter delays for smaller files = faster completion
     */
    private long calculateOptimalDelay(int totalCount) {
        if (totalCount <= SMALL_FILE_THRESHOLD) {
            return 0; // No delay
        } else if (totalCount <= MEDIUM_FILE_THRESHOLD) {
            return 8; // ~2 frames
        } else {
            return 16; // 1 frame for large files
        }
    }

    /**
     * Process small files directly without batching for instant response
     * Disables drawing during selection for maximum performance
     */
    private void processSmallFileDirectly(int count, boolean checked, MenuItem item) {
        // Temporarily disable drawing for ultra-fast selection
        mListView.setDrawingCacheEnabled(false);

        for (int i = 0; i < count; i++) {
            if (mFirstSelection == i && !checked)
                continue;
            mListView.setItemChecked(i, checked);
        }

        // Re-enable drawing and invalidate to show results
        mListView.setDrawingCacheEnabled(true);
        mListView.invalidate();

        finishSelectAll(item);
    }

    /**
     * Process selection in batches to prevent OOM and ANR
     * Uses adaptive delay based on file size for optimal performance
     */
    private void processBatch(final int start, final int total, final int batchSize,
                              final long batchDelay, final boolean checked, final MenuItem item) {
        // Stop processing if action mode was destroyed
        if (mCurrentActionMode == null) {
            mIsSelectingAll = false;
            if (mProgress.isShowing())
                mProgress.dismiss();
            return;
        }

        final int end = Math.min(start + batchSize, total);

        // Process current batch with optimized loop
        for (int i = start; i < end; i++) {
            if (mFirstSelection == i && !checked)
                continue;
            mListView.setItemChecked(i, checked);
        }

        // Continue with next batch or finish
        if (end < total) {
            // Use adaptive delay: no delay for fast files, minimal for medium, 1 frame for large
            if (batchDelay > 0) {
                mActionHandler.postDelayed(() -> processBatch(end, total, batchSize, batchDelay, checked, item), batchDelay);
            } else {
                // Zero delay = use post() for immediate scheduling (faster than postDelayed(0))
                mActionHandler.post(() -> processBatch(end, total, batchSize, batchDelay, checked, item));
            }
        } else {
            // All items processed, update UI once
            finishSelectAll(item);
        }
    }

    /**
     * Finish select all operation and update UI
     */
    private void finishSelectAll(final MenuItem item) {
        // Check if action mode was destroyed during batch processing
        if (mCurrentActionMode == null) {
            mIsSelectingAll = false;
            mBatchUpdateCounter = 0;
            if (mProgress.isShowing())
                mProgress.dismiss();
            return;
        }

        mIsSelectingAll = false;
        mBatchUpdateCounter = 0;

        // Update UI only once after all selections complete
        if (item != null) {
            item.setCheckable(true);
            item.setChecked(mAdapter.getSelectedCount() == mAdapter.getCount());
            View view = item.getActionView();
            if (view != null) {
                view.clearAnimation();
                item.setActionView(null);
            }
        }

        // Update action mode title one final time
        final int checkedCount = mListView.getCheckedItemCount();
        mCurrentActionMode.setTitle(String.format(mActivity.getString(R.string.items_selected), checkedCount));

        if (mMenuItemSelectAll != null)
            mMenuItemSelectAll.setChecked(mAdapter.getSelectedCount() == mAdapter.getCount());

        if (mProgress.isShowing())
            mProgress.dismiss();
    }

    /**
     * Copy action.
     *
     * @param mode The ActionMode providing the selection mode.
     * @return false on error.
     */
    protected abstract boolean actionCopy(ActionMode mode);

    /**
     * Clear action.
     *
     * @param item The item that was clicked.
     * @param mode The ActionMode providing the selection mode.
     */
    protected abstract void actionClear(MenuItem item, ActionMode mode);

    /**
     * Edit action.
     *
     * @param mode The ActionMode providing the selection mode.
     * @return false on error.
     */
    protected abstract boolean actionEdit(ActionMode mode);

    /**
     * Closing the action mode.
     *
     * @param mode    The ActionMode providing the selection mode.
     * @param delayed Delayed ?
     */
    protected void closeActionMode(ActionMode mode, boolean delayed) {
        if (delayed)
            mActionHandler.postDelayed(mode::finish, 500);
        else
            mode.finish();
    }

    /**
     * Displays an error message.
     *
     * @param message The message.
     */
    protected void displayError(@StringRes int message) {
        UIHelper.showErrorDialog(mActivity, mActivity.getString(R.string.error_title), mActivity.getString(message));
    }

    /**
     * Sets the action view.
     *
     * @param item   MenuItem
     * @param action Runnable
     */
    protected void setActionView(final MenuItem item, final Runnable action) {
        UIHelper.showCircularProgressDialog(mProgress);
        mActionHandler.postDelayed(() -> {
            action.run();
            if (item != null) {
                item.setCheckable(true);
                item.setChecked(mAdapter.getSelectedCount() == mAdapter.getCount());
                View view = item.getActionView();
                if (view != null) {
                    view.clearAnimation();
                    item.setActionView(null);
                }
            }
            mProgress.dismiss();
        }, 500);
    }

    /**
     * Copy sb to Android clipboard then close action mode.
     *
     * @param logTitle Title in log.
     * @param mode     ActionMode
     * @param sb       String to copy.
     * @return false in case on error
     */
    protected boolean copyAndClose(String logTitle,
                                   ActionMode mode,
                                   StringBuilder sb) {
        try {
            ClipData clip = ClipData.newPlainText(mActivity.getString(R.string.app_name), sb);
            mClipboard.setPrimaryClip(clip);
        } catch (Exception exception) {
            MyApplication.addLog(mActivity, logTitle,
                    "E: TransactionTooLargeException size: " + sb.toString().length());
            displayError(R.string.error_too_many_text_copied);
            return false;
        }
        UIHelper.toast(mActivity, String.format(mActivity.getString(R.string.text_copied),
                SysHelper.sizeToHuman(mActivity, sb.length(), true, true, false)));
        closeActionMode(mode, true);
        return true;
    }
}

