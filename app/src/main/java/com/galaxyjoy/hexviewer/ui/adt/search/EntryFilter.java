/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Custom filter
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.adt.search;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.galaxyjoy.hexviewer.models.LineEntries;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.adt.config.UserConfig;

public class EntryFilter extends Filter {
    private final SearchableFilterFactory mFilterFactory;
    private final ArrayAdapter<LineEntry> mAdapter;
    private final LineEntries mLineEntries;

    public EntryFilter(final Context context,
            ArrayAdapter<LineEntry> adapter,
            ISearchFrom searchFrom,
            LineEntries lineEntries,
            UserConfig userConfigPortrait,
            UserConfig userConfigLandscape) {
        mAdapter = adapter;
        mLineEntries = lineEntries;
        mFilterFactory = new SearchableFilterFactory(context,
                searchFrom,
                userConfigPortrait,
                userConfigLandscape);
    }

    public void apply(CharSequence constraint, final Set<Integer> tempList) {
        boolean clear = (constraint == null || constraint.length() == 0);
        String query = "";
        final Locale loc = Locale.getDefault();
        if (!clear)
            query = constraint.toString().toLowerCase(loc);
        // Master-Level Fix: Use a thread-safe snapshot of the data.
        // This avoids ConcurrentModificationException without needing try-catch.
        List<LineEntry> items = mLineEntries.getSnapshot();
        final int length = items.size();
        for (int i = 0; i < length; i++) {
            LineEntry lineEntry = items.get(i);
            if (clear)
                tempList.add(i);
            else {
                mFilterFactory.multilineSearch(lineEntry,
                        items,
                        i,
                        query,
                        loc,
                        tempList);
            }
        }
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        final FilterResults filterResults = new FilterResults();
        final Set<Integer> tempList = new HashSet<>();
        apply(constraint, tempList);
        filterResults.count = tempList.size();
        filterResults.values = tempList;
        return filterResults;
    }

    /**
     * Notify about filtered list to ui
     *
     * @param constraint text
     * @param results    filtered result
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence constraint,
            FilterResults results) {
        // Android may publish a cancelled/stale filter request with no values while
        // the user switches between hex and plain adapters. Ignore that callback;
        // the newer request owns the visible result set.
        if (results == null || !(results.values instanceof Set)) return;
        try {
            List<Integer> li = new ArrayList<>((Set<Integer>) results.values);
            Collections.sort(li);
            mLineEntries.setFilteredList(li);
            mAdapter.notifyDataSetChanged();
        } catch (IllegalStateException e) {
            // ListView is in inconsistent state (e.g., during scroll/touch)
            // The filter results will be applied on next filter request
        }
    }
}
