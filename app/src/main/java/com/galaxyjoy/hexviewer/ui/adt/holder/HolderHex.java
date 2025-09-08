/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * </p>
 *
 * @author Keidan
 * <p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.adt.holder;

import android.widget.TextView;

public class HolderHex {
    private TextView mLineNumbers;
    private TextView mContent;

    public void setLineNumbers(TextView tv) {
        mLineNumbers = tv;
    }

    public void setContent(TextView tv) {
        mContent = tv;
    }

    public TextView getLineNumbers() {
        return mLineNumbers;
    }

    public TextView getContent() {
        return mContent;
    }
}
