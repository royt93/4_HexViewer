package com.galaxyjoy.hexviewer.streaming;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Page-cached random-access read session with bounded heap usage. */
public final class StreamingSession implements AutoCloseable {
    public static final int DEFAULT_PAGE_SIZE = 256 * 1024;
    public static final long DEFAULT_CACHE_BYTES = 8L * 1024L * 1024L;

    private final SeekableDataSource source;
    private final RawPageCache cache;
    private final int pageSize;
    private final AtomicLong generation = new AtomicLong();
    private volatile boolean closed;

    public StreamingSession(SeekableDataSource source) {
        this(source, DEFAULT_PAGE_SIZE, DEFAULT_CACHE_BYTES);
    }

    public StreamingSession(SeekableDataSource source, int pageSize, long cacheBytes) {
        this.source = Objects.requireNonNull(source, "source");
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize must be > 0");
        if (cacheBytes < pageSize) throw new IllegalArgumentException("cacheBytes must fit at least one page");
        this.pageSize = pageSize;
        this.cache = new RawPageCache(cacheBytes);
    }

    public synchronized byte[] read(WindowRange range) throws IOException {
        ensureOpen();
        long length = range.length();
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("A materialized window cannot exceed Integer.MAX_VALUE bytes");
        }
        byte[] result = new byte[(int) length];
        int copied = readAt(range.start(), result, 0, result.length);
        return copied == result.length ? result : Arrays.copyOf(result, Math.max(copied, 0));
    }

    public synchronized int readAt(long position, byte[] destination, int offset, int length) throws IOException {
        FileChannelSeekableDataSource.validateRead(position, destination, offset, length);
        ensureOpen();
        if (length == 0) return 0;
        int total = 0;
        while (total < length) {
            long absolute = position + total;
            if (absolute < position) throw new IOException("Read position overflow");
            long pageIndex = absolute / pageSize;
            int inPageOffset = (int) (absolute % pageSize);
            byte[] page = cache.getShared(pageIndex);
            if (page == null) {
                page = loadPage(pageIndex);
                if (page.length > 0) cache.put(pageIndex, page);
            }
            if (inPageOffset >= page.length) break;
            int amount = Math.min(length - total, page.length - inPageOffset);
            System.arraycopy(page, inPageOffset, destination, offset + total, amount);
            total += amount;
            if (page.length < pageSize) break;
        }
        return total == 0 ? -1 : total;
    }

    private byte[] loadPage(long pageIndex) throws IOException {
        final long start;
        try {
            start = Math.multiplyExact(pageIndex, (long) pageSize);
        } catch (ArithmeticException error) {
            throw new IOException("Page offset overflow", error);
        }
        long size = source.size();
        if (size >= 0 && start >= size) return new byte[0];
        int wanted = size >= 0 ? (int) Math.min(pageSize, size - start) : pageSize;
        byte[] page = new byte[wanted];
        int total = 0;
        while (total < wanted) {
            int read = source.readAt(start + total, page, total, wanted - total);
            if (read < 0) break;
            if (read == 0) throw new IOException("Data source made no progress while reading page " + pageIndex);
            total += read;
        }
        return total == page.length ? page : Arrays.copyOf(page, total);
    }

    /** Invalidates pending UI work and returns its new generation token. */
    public long nextGeneration() {
        return generation.incrementAndGet();
    }

    public boolean isCurrentGeneration(long token) {
        return token == generation.get();
    }

    public int pageSize() {
        return pageSize;
    }

    public RawPageCache cache() {
        return cache;
    }

    /** Returns the resolved source length without materializing file contents. */
    public synchronized long size() throws IOException {
        ensureOpen();
        return source.size();
    }

    private void ensureOpen() throws IOException {
        if (closed) throw new IOException("Streaming session is closed");
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        generation.incrementAndGet();
        cache.clear();
        source.close();
    }
}
