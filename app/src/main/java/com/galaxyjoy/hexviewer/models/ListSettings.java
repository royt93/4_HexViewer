package com.galaxyjoy.hexviewer.models;

import android.content.SharedPreferences;

import androidx.annotation.StringRes;

import com.galaxyjoy.hexviewer.MyApplication;

public class ListSettings {
    private final String mKeyDisplayDataColumn;
    private final String mKeyRowHeight;
    private final String mKeyRowHeightAuto;
    private final String mKeyFontSize;
    private String mDefaultRowHeight = "100";
    private boolean mDefaultDisplayDataColumn = true;
    private boolean mDefaultRowHeightAuto = true;
    private String mDefaultFontSize = "16";
    private final MyApplication mApp;

    public ListSettings(final MyApplication app,
                        final String keyDisplayDataColumn,
                        final String keyRowHeight,
                        String keyRowHeightAuto,
                        String keyFontSize
    ) {
        mApp = app;

        mKeyDisplayDataColumn = keyDisplayDataColumn;
        mKeyRowHeight = keyRowHeight;
        mKeyRowHeightAuto = keyRowHeightAuto;
        mKeyFontSize = keyFontSize;
    }

    /**
     * Loads default values.
     *
     * @param e SharedPreferences.Editor
     */
    public void loadDefaultValues(SharedPreferences.Editor e) {
        e.putString(mKeyFontSize, String.valueOf(mDefaultFontSize));
        e.putBoolean(mKeyDisplayDataColumn, mDefaultDisplayDataColumn);
        e.putBoolean(mKeyRowHeightAuto, mDefaultRowHeightAuto);
        e.putString(mKeyRowHeight, String.valueOf(mDefaultRowHeight));
    }

    /**
     * Sets the default values.
     *
     * @param defaultDisplayDataColumn Xml rss
     * @param defaultRowHeight         Xml rss
     * @param defaultRowHeightAuto     Xml rss
     * @param defaultFontSize          Xml rss
     */
    public void setDefaults(final @StringRes int defaultDisplayDataColumn,
                            final @StringRes int defaultRowHeight,
                            final @StringRes int defaultRowHeightAuto,
                            final @StringRes int defaultFontSize) {
        mDefaultDisplayDataColumn = defaultDisplayDataColumn == 0 || Boolean.parseBoolean(mApp.getString(defaultDisplayDataColumn));
        mDefaultRowHeightAuto = Boolean.parseBoolean(mApp.getString(defaultRowHeightAuto));
        mDefaultRowHeight = mApp.getString(defaultRowHeight);
        mDefaultFontSize = mApp.getString(defaultFontSize);
    }

    /**
     * Tests if the data column should be displayed (only available with the hexadecimal list).
     *
     * @return boolean
     */
    public boolean isDisplayDataColumn() {
        try {
            if (mKeyDisplayDataColumn == null)
                return true;
            return mApp.getPref(mApp).getBoolean(mKeyDisplayDataColumn, mDefaultDisplayDataColumn);
        } catch (Exception ignore) {
            return mDefaultDisplayDataColumn;
        }
    }

    /**
     * Returns the row height auto state for the list view.
     *
     * @return boolean
     */
    public boolean isRowHeightAuto() {
        try {
            return mApp.getPref(mApp).getBoolean(mKeyRowHeightAuto, mDefaultRowHeightAuto);
        } catch (Exception ignore) {
            return mDefaultRowHeightAuto;
        }
    }

    /**
     * Returns the row height for the list view.
     *
     * @return int
     */
    public int getRowHeight() {
        try {
            return Integer.parseInt(mApp.getPref(mApp).getString(mKeyRowHeight, mDefaultRowHeight));
        } catch (Exception ignore) {
            return Integer.parseInt(mDefaultRowHeight);
        }
    }

    /**
     * Change the row height for the list view.
     *
     * @param number The new number.
     */
    public void setRowHeight(int number) {
        SharedPreferences.Editor e = mApp.getPref(mApp).edit();
        e.putString(mKeyRowHeight, String.valueOf(number));
        e.apply();
    }

    /**
     * Returns the font size for the list view.
     *
     * @return float
     */
    public float getFontSize() {
        try {
            return Float.parseFloat(mApp.getPref(mApp).getString(mKeyFontSize, mDefaultFontSize));
        } catch (Exception ignore) {
            return Float.parseFloat(mDefaultFontSize);
        }
    }

    /**
     * Change the font size for the list view.
     *
     * @param number The new number.
     */
    public void setFontSize(float number) {
        SharedPreferences.Editor e = mApp.getPref(mApp).edit();
        e.putString(mKeyFontSize, String.valueOf(number));
        e.apply();
    }

}
