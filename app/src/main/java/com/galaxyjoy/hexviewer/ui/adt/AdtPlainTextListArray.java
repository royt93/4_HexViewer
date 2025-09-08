/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.adt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.adt.config.UserConfig;
import com.galaxyjoy.hexviewer.util.SysHelper;

import java.util.List;

public class AdtPlainTextListArray extends AdtSearchableListArray {
    private static final int ID = R.layout.v_listview_simple_row;

    public AdtPlainTextListArray(final Context context,
                                 final List<LineEntry> objects,
                                 UserConfig userConfigPortrait,
                                 UserConfig userConfigLandscape) {
        super(context, ID, objects, userConfigPortrait, userConfigLandscape);
    }

    /**
     * Test if we aren't from the hex view or the plain view.
     *
     * @return boolean
     */
    @Override
    public boolean isSearchNotFromHexView() {
        return true;
    }

    /**
     * Inflate the view.
     *
     * @param convertView This value may be null.
     * @return The view.
     */
    protected @NonNull
    View inflateView(final View convertView) {
        View v = convertView;
        if (v == null) {
            final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (inflater != null) {
                v = inflater.inflate(ID, null);
                final TextView label1 = v.findViewById(R.id.label1);
                v.setTag(label1);
            }
        }
        return v == null ? new View(getContext()) : v;
    }

    private int getContentTextColor(final int position) {
        return isSelected(position) ? R.color.colorPrimaryDark : R.color.textColor;
    }

    private int getContentBackgroundColor(final int position) {
        return isSelected(position) ? R.color.colorAccent : R.color.windowBackground;
    }

    /**
     * Fills the view.
     *
     * @param v        This can't be null.
     * @param position The position of the item within the adapter's data set of the item whose view we want.
     */
    @Override
    protected void fillView(final @NonNull View v, final int position) {
        if (v.getTag() != null) {
            final TextView holder = (TextView) v.getTag();
            LineEntry le = getItem(position);
            if (le != null) {
                holder.setText(SysHelper.ignoreNonDisplayedChar(le.getPlain()));
                holder.setTextColor(ContextCompat.getColor(getContext(), getContentTextColor(position)));
                applyUserConfig(holder);
                v.setBackgroundColor(ContextCompat.getColor(getContext(), getContentBackgroundColor(position)));
            }
        }
    }
}
