package com.galaxyjoy.hexviewer.streaming;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RawPageCacheTest {
    @Test
    public void evictsLeastRecentlyUsedPageByByteBudget() {
        RawPageCache cache = new RawPageCache(6);
        cache.put(1, new byte[]{1, 1});
        cache.put(2, new byte[]{2, 2});
        cache.get(1); // page 2 is now least recently used
        cache.put(3, new byte[]{3, 3, 3});

        assertNull(cache.get(2));
        assertArrayEquals(new byte[]{1, 1}, cache.get(1));
        assertArrayEquals(new byte[]{3, 3, 3}, cache.get(3));
        assertEquals(5, cache.byteCount());
    }

    @Test
    public void copiesValuesAtCacheBoundary() {
        RawPageCache cache = new RawPageCache(10);
        byte[] original = {1, 2, 3};
        cache.put(0, original);
        original[0] = 9;
        byte[] returned = cache.get(0);
        returned[1] = 9;

        assertArrayEquals(new byte[]{1, 2, 3}, cache.get(0));
    }

    @Test
    public void doesNotCachePageLargerThanBudget() {
        RawPageCache cache = new RawPageCache(2);
        cache.put(0, new byte[]{1, 2, 3});

        assertEquals(0, cache.pageCount());
        assertEquals(0, cache.byteCount());
    }
}
