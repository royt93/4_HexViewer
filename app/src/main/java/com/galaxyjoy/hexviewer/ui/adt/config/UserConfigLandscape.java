package com.galaxyjoy.hexviewer.ui.adt.config;

import android.content.Context;

import com.galaxyjoy.hexviewer.MyApplication;

public class UserConfigLandscape implements UserConfig {
    private final MyApplication mApp;
    private final boolean mIsHexList;

    public UserConfigLandscape(final Context ctx, boolean isHexList) {
        mIsHexList = isHexList;
        mApp = (MyApplication) ctx.getApplicationContext();
    }

    @Override
    public float getFontSize() {
        if (mIsHexList) {
            if (mApp.isLineNumber())
                return mApp.getListSettingsHexLineNumbersLandscape().getFontSize();
            return mApp.getListSettingsHexLandscape().getFontSize();
        }
        return mApp.getListSettingsPlainLandscape().getFontSize();
    }

    @Override
    public int getRowHeight() {
        if (mIsHexList) {
            if (mApp.isLineNumber())
                return mApp.getListSettingsHexLineNumbersLandscape().getRowHeight();
            return mApp.getListSettingsHexLandscape().getRowHeight();
        }
        return mApp.getListSettingsPlainLandscape().getRowHeight();
    }

    @Override
    public boolean isRowHeightAuto() {
        if (mIsHexList) {
            if (mApp.isLineNumber())
                return mApp.getListSettingsHexLineNumbersLandscape().isRowHeightAuto();
            return mApp.getListSettingsHexLandscape().isRowHeightAuto();
        }
        return mApp.getListSettingsPlainLandscape().isRowHeightAuto();
    }

    @Override
    public boolean isDataColumnNotDisplayed() {
        if (mIsHexList) {
            if (mApp.isLineNumber())
                return !mApp.getListSettingsHexLineNumbersLandscape().isDisplayDataColumn();
            return !mApp.getListSettingsHexLandscape().isDisplayDataColumn();
        }
        return false;
    }
}
