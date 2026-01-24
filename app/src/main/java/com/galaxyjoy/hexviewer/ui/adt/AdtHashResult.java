package com.galaxyjoy.hexviewer.ui.adt;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.ui.act.ActHashCalculator;

import java.util.ArrayList;
import java.util.List;

public class AdtHashResult extends RecyclerView.Adapter<AdtHashResult.ViewHolder> {

    public static class HashItem {
        public String algorithm;
        public String value;
        public boolean isMatched = false;

        public HashItem(String algorithm, String value) {
            this.algorithm = algorithm;
            this.value = value;
        }
    }

    private List<HashItem> mData = new ArrayList<>();
    private final Context mContext;

    public AdtHashResult(Context context) {
        this.mContext = context;
    }

    public void setData(List<HashItem> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    public boolean updateMatchStatus(String compareString) {
        boolean anyChange = false;
        boolean anyMatch = false;
        for (HashItem item : mData) {
            boolean matches = !compareString.isEmpty() && item.value.equalsIgnoreCase(compareString.trim());
            if (matches)
                anyMatch = true;
            if (item.isMatched != matches) {
                item.isMatched = matches;
                anyChange = true;
            }
        }
        if (anyChange) {
            notifyDataSetChanged();
        }
        return anyMatch;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hash_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HashItem item = mData.get(position);
        holder.tvAlgorithm.setText(item.algorithm);
        holder.tvHashValue.setText(item.value);

        holder.btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Hash", item.value);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(mContext, R.string.hash_copy_toast, Toast.LENGTH_SHORT).show();
        });

        if (item.isMatched) {
            // MATCHED: Green Background + Green Highlighted Text
            if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
                // Light Green 50
                ((com.google.android.material.card.MaterialCardView) holder.itemView)
                        .setCardBackgroundColor(0xFFE8F5E9);
                ((com.google.android.material.card.MaterialCardView) holder.itemView).setStrokeWidth(0);
            }
            holder.tvHashValue.setTextColor(holder.itemView.getContext().getColor(R.color.colorResultSuccess));
            holder.tvHashValue.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            // NORMAL: White Background + Default Text
            if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
                // White
                ((com.google.android.material.card.MaterialCardView) holder.itemView)
                        .setCardBackgroundColor(0xFFFFFFFF);
                ((com.google.android.material.card.MaterialCardView) holder.itemView).setStrokeWidth(0);
            }
            holder.tvHashValue.setTextColor(holder.itemView.getContext().getColor(R.color.textColorPrimary));
            holder.tvHashValue.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlgorithm;
        TextView tvHashValue;
        View btnCopy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlgorithm = itemView.findViewById(R.id.tvAlgorithm);
            tvHashValue = itemView.findViewById(R.id.tvHashValue);
            btnCopy = itemView.findViewById(R.id.btnCopy);
        }
    }
}
