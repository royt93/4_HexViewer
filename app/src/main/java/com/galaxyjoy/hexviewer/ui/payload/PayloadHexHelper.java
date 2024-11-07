package com.galaxyjoy.hexviewer.ui.payload;

import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.ui.act.MainAct;
import com.galaxyjoy.hexviewer.ui.adt.AdtHexTextArray;
import com.galaxyjoy.hexviewer.ui.adt.config.UserConfigLandscape;
import com.galaxyjoy.hexviewer.ui.adt.config.UserConfigPortrait;
import com.galaxyjoy.hexviewer.ui.adt.holder.LineNumbersTitle;
import com.galaxyjoy.hexviewer.ui.multiChoice.HexMultiChoiceCallback;

import java.util.ArrayList;

public class PayloadHexHelper {
    private MainAct mActivity;
    private ListView mPayloadHex = null;
    private AdtHexTextArray mAdapterHex = null;
    private RelativeLayout mPayloadViewContainer = null;
    private LinearLayout mTitle = null;
    private TextView mTitleLineNumbers = null;
    private TextView mTitleContent = null;
    private MyApplication mApp = null;

    /**
     * Called when the activity is created.
     *
     * @param activity The owner activity
     */
    public void onCreate(final MainAct activity) {
        mActivity = activity;
        mApp = (MyApplication) activity.getApplicationContext();
        mPayloadViewContainer = activity.findViewById(R.id.payloadViewContainer);
        mTitle = activity.findViewById(R.id.title);
        mTitleLineNumbers = activity.findViewById(R.id.titleLineNumbers);
        mTitleContent = activity.findViewById(R.id.titleContent);
        mPayloadHex = activity.findViewById(R.id.payloadView);

        mPayloadHex.setVisibility(View.GONE);
        mPayloadViewContainer.setVisibility(View.GONE);
        mTitleLineNumbers.setVisibility(View.GONE);
        mTitleContent.setVisibility(View.GONE);
        mTitle.setVisibility(View.GONE);

        LineNumbersTitle title = new LineNumbersTitle();
        title.setTitleContent(mTitleContent);
        title.setTitleLineNumbers(mTitleLineNumbers);

        mAdapterHex = new AdtHexTextArray(activity,
                new ArrayList<>(),
                title,
                new UserConfigPortrait(activity, true),
                new UserConfigLandscape(activity, true));
        mPayloadHex.setAdapter(mAdapterHex);
        mPayloadHex.setOnItemClickListener(activity);
        mPayloadHex.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        HexMultiChoiceCallback hexMultiChoiceCallback = new HexMultiChoiceCallback(activity, mPayloadHex, mAdapterHex);
        mPayloadHex.setMultiChoiceModeListener(hexMultiChoiceCallback);
    }

    /**
     * Resets the update status.
     */
    public void resetUpdateStatus() {
        String query = mActivity.getSearchQuery();
        if (!query.isEmpty())
            mAdapterHex.manualFilterUpdate(""); /* reset filter */
        mAdapterHex.getEntries().clearFilteredUpdated();
        if (!query.isEmpty())
            mAdapterHex.manualFilterUpdate(query);
        mAdapterHex.notifyDataSetChanged();
    }

    /**
     * Called to refresh the adapter.
     */
    public void refreshAdapter() {
        mAdapterHex.refresh();
    }

    /**
     * Called to refresh the line numbers.
     */
    public void refreshLineNumbers() {
        refreshLineNumbersVisibility();
        mAdapterHex.notifyDataSetChanged();
    }

    /**
     * Returns the hex adapter.
     *
     * @return HexTextArrayAdapter
     */
    public AdtHexTextArray getAdapter() {
        return mAdapterHex;
    }

    /**
     * Tests if the list view is visible.
     *
     * @return boolean
     */
    public boolean isVisible() {
        return mPayloadHex.getVisibility() == View.VISIBLE;
    }

    /**
     * Changes the list view visibility.
     *
     * @param b The new value
     */
    public void setVisible(boolean b) {
        mPayloadHex.setVisibility(b ? View.VISIBLE : View.GONE);
        mPayloadViewContainer.setVisibility(b ? View.VISIBLE : View.GONE);
        if (!b) {
            mTitleLineNumbers.setVisibility(View.GONE);
            mTitleContent.setVisibility(View.GONE);
            mTitle.setVisibility(View.GONE);
        } else
            refreshLineNumbersVisibility();
    }

    /**
     * Refreshes line numbers visibility.
     */
    private void refreshLineNumbersVisibility() {
        final boolean checked = mApp.isLineNumber();
        mTitleLineNumbers.setVisibility(checked ? View.VISIBLE : View.GONE);
        mTitleContent.setVisibility(checked ? View.VISIBLE : View.GONE);
        mTitle.setVisibility(checked ? View.VISIBLE : View.GONE);
    }

    /**
     * Returns the ListView
     *
     * @return ListView
     */
    public ListView getListView() {
        return mPayloadHex;
    }
}
