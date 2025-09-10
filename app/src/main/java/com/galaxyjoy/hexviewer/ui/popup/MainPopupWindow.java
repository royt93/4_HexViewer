/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * PopupWindow used with MainActivity.
 * </p>
 *
 * @author Keidan
 *
 * License: GPLv3
 * <p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.popup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.ui.undoredo.UnDoRedo;
import com.galaxyjoy.hexviewer.util.SysHelper;

public class MainPopupWindow {
    private final PopupWindow mPopup;
    private final TextView mGoTo;
    private final TextView mSaveMenu;
    private final TextView mSaveAsMenu;
    private final TextView mCloseMenu;
    private final TextView mRecentlyOpen;
    private final PopupCheckboxHelper mPlainText;
    private final PopupCheckboxHelper mLineNumbers;
    private final MyApplication mApp;

    public interface ClickListener {
        void onClick(int id);
    }

    public MainPopupWindow(final Context ctx,
                           UnDoRedo undoRedo,
                           ClickListener clickListener) {
        mApp = (MyApplication) ctx.getApplicationContext();

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.dlg_main_popup, null);
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width = popupView.getMeasuredWidth();

        mPopup = new PopupWindow(popupView,
                width + 150,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

        mPopup.setElevation(5.0f);
        mPopup.setOutsideTouchable(true);

        mPlainText = new PopupCheckboxHelper(popupView,
                R.id.actionPlainTextContainer,
                R.id.actionPlainTextTv,
                R.id.actionPlainTextCb);

        mLineNumbers = new PopupCheckboxHelper(popupView,
                R.id.actionLineNumbersContainer,
                R.id.actionLineNumbersTv,
                R.id.actionLineNumbersCb);
        mSaveAsMenu = popupView.findViewById(R.id.actionSaveAs);
        mSaveMenu = popupView.findViewById(R.id.actionSave);
        mCloseMenu = popupView.findViewById(R.id.actionClose);
        mRecentlyOpen = popupView.findViewById(R.id.actionRecentlyOpen);
        ImageView actionRedo = popupView.findViewById(R.id.actionRedo);
        ImageView actionUndo = popupView.findViewById(R.id.actionUndo);
        FrameLayout containerRedo = popupView.findViewById(R.id.containerRedo);
        FrameLayout containerUndo = popupView.findViewById(R.id.containerUndo);
        mGoTo = popupView.findViewById(R.id.actionGoTo);

        View.OnClickListener click = v -> {
            mPopup.dismiss();
            if (clickListener != null)
                clickListener.onClick(v.getId());
        };
        popupView.findViewById(R.id.actionOpen).setOnClickListener(click);
        popupView.findViewById(R.id.actionOpenSequential).setOnClickListener(click);
        popupView.findViewById(R.id.actionSettings).setOnClickListener(click);
        popupView.findViewById(R.id.actionRate).setOnClickListener(click);
        popupView.findViewById(R.id.actionMoreApp).setOnClickListener(click);
        popupView.findViewById(R.id.actionShareApp).setOnClickListener(click);
        popupView.findViewById(R.id.actionAbout).setOnClickListener(click);
        popupView.findViewById(R.id.actionGithubOriginal).setOnClickListener(click);
        popupView.findViewById(R.id.actionGithubFork).setOnClickListener(click);
        mPlainText.setOnClickListener(click);
        mLineNumbers.setOnClickListener(click);
        mSaveAsMenu.setOnClickListener(click);
        mSaveMenu.setOnClickListener(click);
        mCloseMenu.setOnClickListener(click);
        mRecentlyOpen.setOnClickListener(click);
        mGoTo.setOnClickListener(click);
        actionRedo.setOnClickListener(click);
        actionUndo.setOnClickListener(click);
        undoRedo.setControls(containerUndo, actionUndo, containerRedo, actionRedo);
        mLineNumbers.setChecked(mApp.isLineNumber());

        refreshGoToName();
    }

    /**
     * Shows the popup
     *
     * @param more Button "more"
     */
    public void show(View more) {
        if (mPopup != null) {
            mPopup.showAtLocation(more, Gravity.TOP | (SysHelper.isRTL(more) ? Gravity.START : Gravity.END), 12, 120);
        }
    }

    /**
     * Dismiss the popup.
     */
    public void dismiss() {
        if (mPopup != null && mPopup.isShowing())
            mPopup.dismiss();
    }

    /**
     * Returns the menu RecentlyOpen
     *
     * @return MenuItem
     */
    public TextView getMenuRecentlyOpen() {
        return mRecentlyOpen;
    }

    /**
     * Enables/Disables the save as menu.
     *
     * @param en boolean
     */
    public void setMenusEnable(boolean en) {
        setMenuEnabled(mGoTo, en);
        setMenuEnabled(mSaveAsMenu, en);
        setMenuEnabled(mCloseMenu, en);
        setMenuEnabled(mRecentlyOpen, !mApp.getRecentlyOpened().list().isEmpty());
        if (mLineNumbers != null)
            mLineNumbers.setEnable(en);
        if (!en && mPlainText != null) {
            mPlainText.setChecked(false);
            mPlainText.setEnable(false);
        }
    }

    /**
     * Refreshes go to menu.
     */
    public void refreshGoToName() {
        if (mGoTo != null) {
            if (mLineNumbers != null && mLineNumbers.isChecked())
                mGoTo.setText(R.string.action_go_to_address);
            else
                mGoTo.setText(R.string.action_go_to_line);
        }
    }

    /**
     * Enables/Disables the save menu.
     *
     * @param en boolean
     */
    public void setSaveMenuEnable(boolean en) {
        setMenuEnabled(mSaveMenu, en);
    }

    /**
     * Sets whether the menu item is enabled.
     *
     * @param menu    MenuItem
     * @param enabled If true then the item will be invokable; if false it is won't be invokable.
     */
    private void setMenuEnabled(final TextView menu,
                                final boolean enabled) {
        if (menu != null)
            menu.setEnabled(enabled);
    }

    /**
     * Returns PopupCheckboxHelper for plain text.
     *
     * @return PopupCheckboxHelper
     */
    public PopupCheckboxHelper getPlainText() {
        return mPlainText;
    }

    /**
     * Returns PopupCheckboxHelper for line numbers.
     *
     * @return PopupCheckboxHelper
     */
    public PopupCheckboxHelper getLineNumbers() {
        return mLineNumbers;
    }
}
