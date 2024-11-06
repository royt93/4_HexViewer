package com.galaxyjoy.hexviewer.ui.adt.search;

import java.util.Set;

public class SearchResult {
    private final int mLength;
    private final Set<Integer> mIndexes;
    private final boolean mHexPart;
    private final boolean mWithSpaces;
    private final boolean mNotFromHexView;

    protected SearchResult(int length,
                           Set<Integer> indexes,
                           boolean hexPart,
                           boolean withSpaces,
                           boolean notFromHexView) {
        mLength = length;
        mIndexes = indexes;
        mHexPart = hexPart;
        mWithSpaces = withSpaces;
        mNotFromHexView = notFromHexView;
    }

    /**
     * Returns the length.
     *
     * @return int
     */
    public int getLength() {
        return mLength;
    }

    /**
     * Returns the indexes.
     *
     * @return Set<Integer>
     */
    public Set<Integer> getIndexes() {
        return mIndexes;
    }

    /**
     * Tests if the indexes are in the hexadecimal part.
     *
     * @return boolean
     */
    public boolean isHexPart() {
        return mHexPart;
    }

    /**
     * Tests if the indexes are in a search with spaces or not (only if isHexPart == true).
     *
     * @return boolean
     */
    public boolean isWithSpaces() {
        return mWithSpaces;
    }

    /**
     * Tests if the indexes aren't in the hexadecimal part.
     *
     * @return boolean
     */
    public boolean isNotFromHexView() {
        return mNotFromHexView;
    }
}
