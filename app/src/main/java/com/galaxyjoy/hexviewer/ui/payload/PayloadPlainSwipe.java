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
    private ActMain mActivity;
    private ListView mPayloadPlain = null;
    private AdtPlainTextListArray mAdapterPlain = null;
    private SwipeRefreshLayout mPayloadPlainSwipeRefreshLayout;
    private final AtomicBoolean mCancelPayloadPlainSwipeRefresh = new AtomicBoolean(false);
    private UserConfigPortrait mUserConfigPortrait;
    private UserConfigLandscape mUserConfigLandscape;

    /**
     * Called when the activity is created.
     *
     * @param activity The owner activity
     */
    public void onCreate(final ActMain activity) {
        mActivity = activity;
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
        PlainMultiChoiceCallback plainMultiChoiceCallback = new PlainMultiChoiceCallback(activity, mPayloadPlain, mAdapterPlain);
        mPayloadPlain.setMultiChoiceModeListener(plainMultiChoiceCallback);
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
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
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
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mCancelPayloadPlainSwipeRefresh.set(false);
            final List<LineEntry> list = refreshPlain(mCancelPayloadPlainSwipeRefresh);
            if (!mCancelPayloadPlainSwipeRefresh.get()) {
                mActivity.runOnUiThread(() -> {
                    mAdapterPlain.clear();
                    mAdapterPlain.addAll(list);
                    if (!mActivity.getSearchQuery().isEmpty())
                        mAdapterPlain.manualFilterUpdate(mActivity.getSearchQuery());
                });
            }
            mPayloadPlainSwipeRefreshLayout.setRefreshing(false);
            mCancelPayloadPlainSwipeRefresh.set(false);
        }, 100);
    }


    /**
     * Refreshes the plain text list according to the list of payload data.
     *
     * @param cancel Used to cancel this method.
     * @return List<ListData < String>>
     */
    private List<LineEntry> refreshPlain(final AtomicBoolean cancel) {
        int maxByLine = UIHelper.getMaxByLine(mActivity, mUserConfigLandscape, mUserConfigPortrait);
        final List<Byte> payload = new ArrayList<>();
        for (LineEntry le : mActivity.getPayloadHex().getAdapter().getEntries().getItems())
            payload.addAll(le.getRaw());
        final StringBuilder sb = new StringBuilder();
        int nbPerLine = 0;
        final List<LineEntry> list = new ArrayList<>();
        for (int i = 0; i < payload.size() && (cancel == null || !cancel.get()); i++) {
            sb.append((char) payload.get(i).byteValue());
            if (nbPerLine != 0 && (nbPerLine % maxByLine) == 0) {
                list.add(new LineEntry(sb.toString(), null));
                nbPerLine = 0;
                sb.setLength(0);
            } else {
                nbPerLine++;
            }
        }
        if ((cancel == null || !cancel.get()) && nbPerLine != 0) {
            list.add(new LineEntry(sb.toString(), null));
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

}
