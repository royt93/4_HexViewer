package com.galaxyjoy.hexviewer.streaming;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.galaxyjoy.hexviewer.streaming.fixture.PatternFileFactory;
import com.galaxyjoy.hexviewer.streaming.fixture.StreamingTestContentProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** On-device contract tests joining a real content FD to the bounded page cache. */
@RunWith(AndroidJUnit4.class)
public class ContentUriStreamingSessionIntegrationTest {
    @Test
    public void largeSeekableContentUri_readsStartMiddleAndTailWithinCacheBudget() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ContentResolver resolver = context.getContentResolver();
        long size = 35L * 1024 * 1024 + 1;
        Uri uri = StreamingTestContentProvider.uri("seekable", "session-large.bin", size);
        long[] markerOffsets = {0, size / 2, size - PatternFileFactory.MARKER_SIZE};

        try (ContentUriSeekableDataSource source = ContentUriSeekableDataSource.open(resolver, uri);
             StreamingSession session = new StreamingSession(source, 4 * 1024, 8 * 1024L)) {
            assertEquals(size, source.size());
            byte[] marker = new byte[PatternFileFactory.MARKER_SIZE];
            for (long markerOffset : markerOffsets) {
                assertEquals(marker.length,
                        session.readAt(markerOffset, marker, 0, marker.length));
                for (int i = 0; i < marker.length; i++) {
                    assertEquals(
                            "Wrong byte at absolute offset " + (markerOffset + i),
                            PatternFileFactory.expectedByte(markerOffset + i),
                            marker[i]
                    );
                }
                assertTrue("Cache exceeded configured byte budget",
                        session.cache().byteCount() <= session.cache().maxBytes());
            }
            assertTrue("Two-page cache must evict old pages", session.cache().pageCount() <= 2);
        }
    }

    @Test
    public void nonSeekableContentUri_isRejectedBeforeStreamingSessionStarts() throws Exception {
        ContentResolver resolver = InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getContentResolver();
        Uri pipe = StreamingTestContentProvider.uri("pipe", "session-pipe.bin", 4096);
        try {
            ContentUriSeekableDataSource.open(resolver, pipe);
            fail("Pipe-backed provider must be rejected as non-seekable");
        } catch (IOException expected) {
            // Random access cannot be implemented safely on a pipe.
        }
    }

    @Test
    public void factory_spoolsPipeAndReusesItForLaterRandomAccessLease() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        long size = 32 * 1024L + 3;
        Uri pipe = StreamingTestContentProvider.uri("pipe", "factory-pipe.bin", size);
        long tail = size - 2;
        try {
            try (SeekableDataSource first = SeekableDataSourceFactory.openContentUri(
                    context.getContentResolver(), pipe, context.getCacheDir(), size)) {
                assertEquals(size, first.size());
                byte[] value = new byte[1];
                assertEquals(1, first.readAt(tail, value, 0, 1));
                assertEquals(PatternFileFactory.expectedByte(tail), value[0]);
            }
            // The completed bytes are reused; only the lightweight seekability probe may reopen.
            try (SeekableDataSource second = SeekableDataSourceFactory.openContentUri(
                    context.getContentResolver(), pipe, context.getCacheDir(), size)) {
                byte[] value = new byte[1];
                assertEquals(1, second.readAt(7, value, 0, 1));
                assertEquals(PatternFileFactory.expectedByte(7), value[0]);
            }
        } finally {
            SeekableDataSourceFactory.releaseContentUri(pipe);
        }
    }

    @Test
    public void close_releasesSourceAndRejectsFurtherReads() throws Exception {
        ContentResolver resolver = InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getContentResolver();
        Uri uri = StreamingTestContentProvider.uri("seekable", "session-close.bin", 8192);
        ContentUriSeekableDataSource source = ContentUriSeekableDataSource.open(resolver, uri);
        StreamingSession session = new StreamingSession(source, 4096, 4096);
        session.readAt(0, new byte[1], 0, 1);
        session.close();
        assertEquals(0, session.cache().byteCount());
        try {
            session.readAt(0, new byte[1], 0, 1);
            fail("Closed session must reject reads");
        } catch (IOException expected) {
            // Expected.
        }
    }
}
