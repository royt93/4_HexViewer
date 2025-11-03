/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Recently opened (settings)
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.galaxyjoy.hexviewer.MyApplication;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RecentlyOpened {
    protected static final String SEQUENTIAL_MASK = "$^#*";
    private static final int MAX_RECENTLY_OPENED = 50; // Maximum number of recently opened files
    private static final int MAX_PREFS_SIZE = 500 * 1024; // 500KB limit for SharedPreferences data
    private List<FileData> mList;
    private final MyApplication mApp;

    public RecentlyOpened(MyApplication app) {
        mApp = app;
        if (!migrate())
            mList = load();
    }

    public void reload() {
        mList.clear();
        mList = load();
    }

    public void clear() {
        SharedPreferences.Editor e = mApp.getPref(mApp).edit();
        e.putString(SettingsKeys.CFG_RECENTLY_OPEN, "");
        e.apply();
        reload();
    }

    private boolean migrate() {
        String content = mApp.getPref(mApp).getString(SettingsKeys.CFG_RECENTLY_OPEN, "");
        if (content.isEmpty() || content.startsWith(SEQUENTIAL_MASK))
            return false;
        final List<FileData> uris = new ArrayList<>();
        String[] split = content.split("\\|");
        if (split.length != 0 && !split[0].isEmpty())
            for (String s : split) {
                uris.add(decode(mApp, s));
            }
        setRecentlyOpened(uris);
        mList = uris;
        return true;
    }

    public static FileData decode(final Context ctx, final String str) {
        String[] split = str.split("\\" + FileData.SEQUENTIAL_SEP);
        if (split.length == 1)
            return new FileData(ctx,
                    Uri.parse(split[0]),
                    false);
        else if (split.length == 2) {
            try {
                return new FileData(ctx,
                        Uri.parse(split[1]),
                        false,
                        0L,
                        Long.parseLong(split[0]));
            } catch (Exception e) {
                Log.e(RecentlyOpened.class.getName(), "RecentlyOpened Exception: " + e.getMessage(), e);
                return new FileData(ctx,
                        Uri.parse(split[1]),
                        false);
            }
        } else {
            try {
                return new FileData(ctx,
                        Uri.parse(split[2]),
                        false,
                        Long.parseLong(split[0]),
                        Long.parseLong(split[1]));
            } catch (Exception e) {
                Log.e(RecentlyOpened.class.getName(), "RecentlyOpened Exception: " + e.getMessage(), e);
                return new FileData(ctx,
                        Uri.parse(split[2]),
                        false);
            }
        }
    }

    /**
     * Adds a new element to the list.
     *
     * @param recent The new element
     */
    public void add(FileData recent) {
        removeElement(recent.toString());
        mList.add(recent);

        // Limit the list size to prevent OOM
        while (mList.size() > MAX_RECENTLY_OPENED) {
            mList.remove(0); // Remove oldest entry
        }

        setRecentlyOpened(mList);
    }

    /**
     * Removes an existing element from the list.
     *
     * @param recent The new element
     */
    public void remove(FileData recent) {
        remove(recent.toString());
    }

    /**
     * Removes an existing element from the list.
     *
     * @param recent The new element
     */
    public void remove(String recent) {
        removeElement(recent);
        setRecentlyOpened(mList);
    }

    private void removeElement(String recent) {
        for (Iterator<FileData> iterator = mList.iterator(); iterator.hasNext(); ) {
            FileData fd = iterator.next();
            if (fd.toString().equals(recent)) {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Returns the list of recently opened files.
     *
     * @return List<FileData>
     */
    public List<FileData> list() {
        return mList;
    }

    /**
     * Sets the list of recently opened files.
     *
     * @param list The list
     */
    private void setRecentlyOpened(List<FileData> list) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(SEQUENTIAL_MASK);
            final int size = list.size();

            // Build the string incrementally and check size
            for (int i = 0; i < size; i++) {
                String item = list.get(i).toString();

                // Check if adding this item would exceed the limit
                if (sb.length() + item.length() + 1 > MAX_PREFS_SIZE) {
                    Log.w(RecentlyOpened.class.getName(),
                        "Recently opened list is too large, truncating at " + i + " items");
                    break;
                }

                sb.append(item);
                if (i != size - 1)
                    sb.append("|");
            }

            SharedPreferences.Editor e = mApp.getPref(mApp).edit();
            e.putString(SettingsKeys.CFG_RECENTLY_OPEN, sb.toString());
            e.apply();
        } catch (OutOfMemoryError oom) {
            Log.e(RecentlyOpened.class.getName(),
                "OutOfMemoryError while saving recently opened files, clearing list", oom);
            // Clear the list to prevent future OOM errors
            try {
                SharedPreferences.Editor e = mApp.getPref(mApp).edit();
                e.putString(SettingsKeys.CFG_RECENTLY_OPEN, SEQUENTIAL_MASK);
                e.apply();
                mList.clear();
            } catch (Exception clearEx) {
                Log.e(RecentlyOpened.class.getName(),
                    "Failed to clear recently opened list", clearEx);
            }
        } catch (Exception e) {
            Log.e(RecentlyOpened.class.getName(),
                "Error while saving recently opened files", e);
        }
    }

    /**
     * Loads the list of recently opened files.
     *
     * @return List<FileData>
     */
    private List<FileData> load() {
        final List<FileData> uris = new ArrayList<>();
        try {
            String content = mApp.getPref(mApp).getString(SettingsKeys.CFG_RECENTLY_OPEN, "");
            if (content.startsWith(SEQUENTIAL_MASK))
                content = content.substring(SEQUENTIAL_MASK.length());
            String[] split = content.split("\\|");
            if (split.length != 0 && !split[0].isEmpty()) {
                int count = 0;
                for (String s : split) {
                    if (count >= MAX_RECENTLY_OPENED) {
                        Log.w(RecentlyOpened.class.getName(),
                            "Loaded list exceeds MAX_RECENTLY_OPENED, truncating at " + MAX_RECENTLY_OPENED);
                        break;
                    }
                    uris.add(decode(mApp, s));
                    count++;
                }
            }
        } catch (OutOfMemoryError oom) {
            Log.e(RecentlyOpened.class.getName(),
                "OutOfMemoryError while loading recently opened files", oom);
            // Clear corrupted data
            try {
                SharedPreferences.Editor e = mApp.getPref(mApp).edit();
                e.putString(SettingsKeys.CFG_RECENTLY_OPEN, SEQUENTIAL_MASK);
                e.apply();
            } catch (Exception clearEx) {
                Log.e(RecentlyOpened.class.getName(),
                    "Failed to clear corrupted recently opened list", clearEx);
            }
        } catch (Exception e) {
            Log.e(RecentlyOpened.class.getName(),
                "Error while loading recently opened files", e);
        }
        return uris;
    }
}
