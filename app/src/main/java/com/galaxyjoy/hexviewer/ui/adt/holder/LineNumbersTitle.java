package com.galaxyjoy.hexviewer.ui.adt.holder;

import android.widget.TextView;

public class LineNumbersTitle {
    private TextView mTitleLineNumbers;
    private TextView mTitleContent;

    public TextView getTitleLineNumbers() {
        return mTitleLineNumbers;
    }

    public void setTitleLineNumbers(TextView mTitleLineNumbers) {
        this.mTitleLineNumbers = mTitleLineNumbers;
    }

    public TextView getTitleContent() {
        return mTitleContent;
    }

    public void setTitleContent(TextView mTitleContent) {
        this.mTitleContent = mTitleContent;
    }
}
