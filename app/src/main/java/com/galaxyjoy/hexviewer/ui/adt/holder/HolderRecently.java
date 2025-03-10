package com.galaxyjoy.hexviewer.ui.adt.holder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.galaxyjoy.hexviewer.R;

public class HolderRecently extends RecyclerView.ViewHolder {
    private TextView mIndex;
    private final TextView mDetail;
    private TextView mName;

    public HolderRecently(View view) {
        super(view);
        mIndex = view.findViewById(R.id.index);
        mDetail = view.findViewById(R.id.detail);
        mName = view.findViewById(R.id.name);
    }

    public TextView getIndex() {
        return mIndex;
    }

    public void setIndex(TextView mIndex) {
        this.mIndex = mIndex;
    }

    public TextView getDetail() {
        return mDetail;
    }

    public TextView getName() {
        return mName;
    }

    public void setName(TextView mName) {
        this.mName = mName;
    }
}
