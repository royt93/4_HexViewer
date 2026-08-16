package com.galaxyjoy.hexviewer.streaming;

import java.util.LinkedHashMap;
import java.util.Map;

/** Thread-safe byte-budgeted LRU cache keyed by page index. */
public final class RawPageCache {
    private final long maxBytes;
    private final LinkedHashMap<Long, byte[]> pages = new LinkedHashMap<>(16, 0.75f, true);
    private long byteCount;

    public RawPageCache(long maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be > 0");
        }
        this.maxBytes = maxBytes;
    }

    public synchronized byte[] get(long pageIndex) {
        byte[] page = getShared(pageIndex);
        return page == null ? null : page.clone();
    }

    /** Internal zero-copy lookup. Callers must never mutate the returned page. */
    synchronized byte[] getShared(long pageIndex) {
        return pages.get(pageIndex);
    }

    public synchronized void put(long pageIndex, byte[] page) {
        if (pageIndex < 0) throw new IllegalArgumentException("pageIndex must be >= 0");
        if (page == null) throw new NullPointerException("page");
        byte[] copy = page.clone();
        byte[] previous = pages.remove(pageIndex);
        if (previous != null) byteCount -= previous.length;
        if (copy.length <= maxBytes) {
            pages.put(pageIndex, copy);
            byteCount += copy.length;
        }
        trimToBudget();
    }

    private void trimToBudget() {
        while (byteCount > maxBytes && !pages.isEmpty()) {
            Map.Entry<Long, byte[]> eldest = pages.entrySet().iterator().next();
            byteCount -= eldest.getValue().length;
            pages.remove(eldest.getKey());
        }
    }

    public synchronized void clear() {
        pages.clear();
        byteCount = 0;
    }

    public synchronized int pageCount() {
        return pages.size();
    }

    public synchronized long byteCount() {
        return byteCount;
    }

    public long maxBytes() {
        return maxBytes;
    }
}
