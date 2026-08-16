package com.galaxyjoy.hexviewer.streaming;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Process-lifetime cache for a non-seekable source, shared by window/search tasks. */
final class PersistentSpoolCache {
    interface Creator {
        File create() throws IOException;
    }

    private static final class Entry {
        private final File file;
        private final long expectedSize;
        private int leases;
        private boolean releaseRequested;

        private Entry(File file, long expectedSize) {
            this.file = file;
            this.expectedSize = expectedSize;
        }
    }

    private final Map<String, Entry> entries = new HashMap<>();

    synchronized SeekableDataSource acquire(String key, long expectedSize, Creator creator)
            throws IOException {
        Entry existing = entries.get(key);
        if (!isValid(existing, expectedSize)) {
            removeEntry(key, existing);
            evictIdleEntriesExcept(key);
            File file = creator.create();
            long actualSize = file.length();
            if (expectedSize >= 0L && actualSize != expectedSize) {
                delete(file);
                throw new IOException("Provider size changed while spooling: expected "
                        + expectedSize + ", copied " + actualSize);
            }
            existing = new Entry(file, expectedSize);
            entries.put(key, existing);
        }
        existing.leases++;
        try {
            return new Lease(key, existing, FileChannelSeekableDataSource.open(existing.file));
        } catch (IOException | RuntimeException error) {
            existing.leases--;
            cleanupReleased(key, existing);
            throw error;
        }
    }

    synchronized SeekableDataSource acquireExisting(String key, long expectedSize)
            throws IOException {
        Entry entry = entries.get(key);
        if (!isValid(entry, expectedSize)) {
            removeEntry(key, entry);
            return null;
        }
        entry.leases++;
        try {
            return new Lease(key, entry, FileChannelSeekableDataSource.open(entry.file));
        } catch (IOException | RuntimeException error) {
            entry.leases--;
            cleanupReleased(key, entry);
            throw error;
        }
    }

    synchronized void release(String key) {
        Entry entry = entries.get(key);
        if (entry == null) return;
        entry.releaseRequested = true;
        cleanupReleased(key, entry);
    }

    synchronized void clear() {
        for (Entry entry : entries.values()) {
            entry.releaseRequested = true;
        }
        Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Entry> item = iterator.next();
            if (item.getValue().leases == 0) {
                delete(item.getValue().file);
                iterator.remove();
            }
        }
    }

    private boolean isValid(Entry entry, long expectedSize) {
        if (entry == null || entry.releaseRequested || !entry.file.isFile()) return false;
        long length = entry.file.length();
        if (entry.expectedSize >= 0L && length != entry.expectedSize) return false;
        return expectedSize < 0L || length == expectedSize;
    }

    private void evictIdleEntriesExcept(String retainedKey) {
        Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Entry> item = iterator.next();
            if (item.getKey().equals(retainedKey)) continue;
            Entry entry = item.getValue();
            entry.releaseRequested = true;
            if (entry.leases == 0) {
                delete(entry.file);
                iterator.remove();
            }
        }
    }

    private void removeEntry(String key, Entry entry) {
        if (entry == null) return;
        entry.releaseRequested = true;
        if (entry.leases == 0) {
            entries.remove(key);
            delete(entry.file);
        }
    }

    private void cleanupReleased(String key, Entry entry) {
        if (entry.releaseRequested && entry.leases == 0) {
            if (entries.get(key) == entry) entries.remove(key);
            delete(entry.file);
        }
    }

    private static void delete(File file) {
        if (file != null && file.exists() && !file.delete()) file.deleteOnExit();
    }

    private final class Lease implements SeekableDataSource {
        private final String key;
        private final Entry entry;
        private final FileChannelSeekableDataSource delegate;
        private boolean closed;

        private Lease(String key, Entry entry, FileChannelSeekableDataSource delegate) {
            this.key = key;
            this.entry = entry;
            this.delegate = delegate;
        }

        @Override public long size() throws IOException { return delegate.size(); }

        @Override
        public int readAt(long position, byte[] destination, int offset, int length)
                throws IOException {
            return delegate.readAt(position, destination, offset, length);
        }

        @Override
        public void close() throws IOException {
            synchronized (PersistentSpoolCache.this) {
                if (closed) return;
                closed = true;
                IOException failure = null;
                try {
                    delegate.close();
                } catch (IOException error) {
                    failure = error;
                } finally {
                    entry.leases--;
                    cleanupReleased(key, entry);
                }
                if (failure != null) throw failure;
            }
        }
    }
}
