/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Swipe management for the plain text display listview
 * </p>
 *
 * @author Keidan
 * <p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.payload;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.galaxyjoy.hexviewer.ui.adt.AdtPlainTextListArray;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.act.ActMain;
import com.galaxyjoy.hexviewer.ui.adt.config.UserConfigLandscape;
import com.galaxyjoy.hexviewer.ui.adt.config.UserConfigPortrait;
import com.galaxyjoy.hexviewer.ui.multiChoice.PlainMultiChoiceCallback;
import com.galaxyjoy.hexviewer.ui.util.UIHelper;

public class PayloadPlainSwipe {
    // Limit number of plain text lines to prevent OOM with very large files
    // Users can still use hex view for large files
    private static final int MAX_PLAIN_TEXT_LINES = 50000;

    private ActMain mActivity;
    private ListView mPayloadPlain = null;
    private AdtPlainTextListArray mAdapterPlain = null;
    private SwipeRefreshLayout mPayloadPlainSwipeRefreshLayout;
    private final AtomicBoolean mCancelPayloadPlainSwipeRefresh = new AtomicBoolean(false);
    private UserConfigPortrait mUserConfigPortrait;
    private UserConfigLandscape mUserConfigLandscape;
    private Handler mRefreshHandler;
    private PlainMultiChoiceCallback mPlainMultiChoiceCallback = null;

    /**
     * Called when the activity is created.
     *
     * @param activity The owner activity
     */
    public void onCreate(final ActMain activity) {
        mActivity = activity;
        mRefreshHandler = new Handler(Looper.getMainLooper());
        mPayloadPlain = activity.findViewById(R.id.payloadPlain);
        mPayloadPlainSwipeRefreshLayout = activity.findViewById(R.id.payloadPlainSwipeRefreshLayout);
        // Configure SwipeRefreshLayout
        mPayloadPlainSwipeRefreshLayout.setOnRefreshListener(this::refresh);
        mPayloadPlainSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_green_light,
                android.R.color.holo_red_light);
        mPayloadPlain.setVisibility(View.GONE);

        mUserConfigPortrait = new UserConfigPortrait(activity, false);
        mUserConfigLandscape = new UserConfigLandscape(activity, false);
        mAdapterPlain = new AdtPlainTextListArray(activity,
                new ArrayList<>(),
                mUserConfigPortrait,
                mUserConfigLandscape);
        mPayloadPlain.setAdapter(mAdapterPlain);
        mPayloadPlain.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mPlainMultiChoiceCallback = new PlainMultiChoiceCallback(activity, mPayloadPlain, mAdapterPlain);
        mPayloadPlain.setMultiChoiceModeListener(mPlainMultiChoiceCallback);
    }

    /**
     * Called to refresh the adapter.
     */
    public void refreshAdapter() {
        refresh();
        mAdapterPlain.refresh();
    }

    /**
     * Gets the list view adapter.
     *
     * @return SearchableListArrayAdapter
     */
    public AdtPlainTextListArray getAdapter() {
        return mAdapterPlain;
    }

    /**
     * Tests if the list view is visible.
     *
     * @return boolean
     */
    public boolean isVisible() {
        return mPayloadPlain.getVisibility() == View.VISIBLE;
    }

    /**
     * Changes the list view visibility.
     *
     * @param b The new value
     */
    public void setVisible(boolean b) {
        mPayloadPlain.setVisibility(b ? View.VISIBLE : View.GONE);
        if (b) {
            mRefreshHandler.postDelayed(() -> {
                mPayloadPlainSwipeRefreshLayout.setRefreshing(true);
                refresh();
            }, 100);
        }
    }

    /**
     * Functions called to refresh the list.
     */
    public void refresh() {
        mCancelPayloadPlainSwipeRefresh.set(true);
        mRefreshHandler.postDelayed(() -> {
            mCancelPayloadPlainSwipeRefresh.set(false);
            final List<LineEntry> list = refreshPlain(mCancelPayloadPlainSwipeRefresh);
            if (!mCancelPayloadPlainSwipeRefresh.get()) {
                mActivity.runOnUiThread(() -> {
                    mAdapterPlain.clear();
                    mAdapterPlain.addAll(list);
                    if (!mActivity.getSearchQuery().isEmpty())
                        mAdapterPlain.getFilter().filter(mActivity.getSearchQuery());
                });
            }
            mPayloadPlainSwipeRefreshLayout.setRefreshing(false);
            mCancelPayloadPlainSwipeRefresh.set(false);
        }, 100);
    }

    /**
     * Refreshes the plain text list according to the list of payload data.
     * Optimized to avoid OOM by processing entries in streaming fashion.
     *
     * @param cancel Used to cancel this method.
     * @return List<ListData < String>>
     */
    private List<LineEntry> refreshPlain(final AtomicBoolean cancel) {
        int maxByLine = UIHelper.getMaxByLine(mActivity, mUserConfigLandscape, mUserConfigPortrait);
        final List<LineEntry> list = new ArrayList<>();
        final StringBuilder sb = new StringBuilder(maxByLine); // Pre-allocate with capacity
        int nbPerLine = 0;
        boolean limitReached = false;

        // Process entries in streaming fashion to avoid creating giant ArrayList<Byte>
        // This prevents OOM with large files by not loading all bytes into memory at
        // once
        List<LineEntry> hexEntries = mActivity.getPayloadHex().getAdapter().getEntries().getItems();

        outerLoop:
        for (LineEntry le : hexEntries) {
            if (cancel != null && cancel.get()) {
                break;
            }

            // Process raw bytes directly without intermediate ArrayList
            List<Byte> rawBytes = le.getRaw();
            if (rawBytes == null)
                continue;

            for (Byte b : rawBytes) {
                if (cancel != null && cancel.get()) {
                    break outerLoop;
                }

                // Check if we reached the limit
                if (list.size() >= MAX_PLAIN_TEXT_LINES) {
                    limitReached = true;
                    break outerLoop;
                }

                sb.append((char) b.byteValue());
                nbPerLine++;

                if (nbPerLine >= maxByLine) {
                    list.add(new LineEntry(sb.toString(), null));
                    nbPerLine = 0;
                    sb.setLength(0);
                }
            }
        }

        // Add remaining characters if any
        if ((cancel == null || !cancel.get()) && nbPerLine > 0 && !limitReached) {
            list.add(new LineEntry(sb.toString(), null));
        }

        // If limit reached, add a warning message
        if (limitReached && list.size() > 0) {
            list.add(
                    new LineEntry("... (File too large, showing first " + MAX_PLAIN_TEXT_LINES + " lines only)", null));
        }

        return list;
    }

    /**
     * Returns the ListView
     *
     * @return ListView
     */
    public ListView getListView() {
        return mPayloadPlain;
    }

    /**
     * Called when the activity is destroyed.
     * Cleans up handlers to prevent memory leaks.
     */
    public void onDestroy() {
        if (mRefreshHandler != null) {
            mRefreshHandler.removeCallbacksAndMessages(null);
        }
        if (mPlainMultiChoiceCallback != null) {
            mPlainMultiChoiceCallback.cleanup();
        }
    }

}
