package com.galaxyjoy.hexviewer.streaming;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class PersistentSpoolCacheTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void reusesCompletedSpoolAcrossSequentialWindowLeases() throws Exception {
        PersistentSpoolCache cache = new PersistentSpoolCache();
        AtomicInteger creates = new AtomicInteger();
        PersistentSpoolCache.Creator creator = () -> createFile("shared.bin", new byte[]{1, 2, 3}, creates);

        try (SeekableDataSource first = cache.acquire("uri", 3, creator)) {
            assertArrayEquals(new byte[]{1, 2, 3}, read(first, 3));
        }
        try (SeekableDataSource second = cache.acquire("uri", 3, creator)) {
            assertArrayEquals(new byte[]{1, 2, 3}, read(second, 3));
        }

        assertEquals(1, creates.get());
        assertTrue(new File(temporaryFolder.getRoot(), "shared.bin").exists());
        cache.release("uri");
        assertFalse(new File(temporaryFolder.getRoot(), "shared.bin").exists());
    }

    @Test
    public void releaseWaitsForLastActiveLeaseBeforeDeleting() throws Exception {
        PersistentSpoolCache cache = new PersistentSpoolCache();
        File file = createFile("leased.bin", new byte[]{7}, new AtomicInteger());
        SeekableDataSource first = cache.acquire("uri", 1, () -> file);
        SeekableDataSource second = cache.acquire("uri", 1, () -> file);

        cache.release("uri");
        first.close();
        assertTrue(file.exists());
        second.close();
        assertFalse(file.exists());
    }

    @Test
    public void differentSourceEvictsPreviousIdleSpool() throws Exception {
        PersistentSpoolCache cache = new PersistentSpoolCache();
        File firstFile = createFile("first.bin", new byte[]{1}, new AtomicInteger());
        try (SeekableDataSource ignored = cache.acquire("first", 1, () -> firstFile)) { }
        File secondFile = createFile("second.bin", new byte[]{2}, new AtomicInteger());

        try (SeekableDataSource ignored = cache.acquire("second", 1, () -> secondFile)) {
            assertFalse(firstFile.exists());
            assertTrue(secondFile.exists());
        }
        cache.clear();
        assertFalse(secondFile.exists());
    }

    @Test
    public void expectedSizeChangeInvalidatesStaleSpool() throws Exception {
        PersistentSpoolCache cache = new PersistentSpoolCache();
        AtomicInteger creates = new AtomicInteger();
        File first = createFile("old.bin", new byte[]{1}, creates);
        try (SeekableDataSource ignored = cache.acquire("uri", 1, () -> first)) { }
        File replacement = createFile("new.bin", new byte[]{2, 3}, creates);

        try (SeekableDataSource source = cache.acquire("uri", 2, () -> replacement)) {
            assertArrayEquals(new byte[]{2, 3}, read(source, 2));
        }
        assertFalse(first.exists());
    }

    private File createFile(String name, byte[] bytes, AtomicInteger creates) throws IOException {
        creates.incrementAndGet();
        File file = new File(temporaryFolder.getRoot(), name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
        }
        return file;
    }

    private static byte[] read(SeekableDataSource source, int length) throws Exception {
        byte[] result = new byte[length];
        assertEquals(length, source.readAt(0, result, 0, length));
        return result;
    }
}
