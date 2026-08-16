package com.galaxyjoy.hexviewer.streaming;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.galaxyjoy.hexviewer.streaming.fixture.PatternFileFactory;
import com.galaxyjoy.hexviewer.streaming.fixture.StreamingTestContentProvider;
import com.galaxyjoy.hexviewer.util.io.FileHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class StreamingTestContentProviderIntegrationTest {
    @Test
    public void seekableUri_reportsMetadataAndSupportsRandomAccess() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ContentResolver resolver = context.getContentResolver();
        long size = 35L * 1024 * 1024 + 1;
        Uri uri = StreamingTestContentProvider.uri("seekable", "large.bin", size);

        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            assertNotNull(cursor);
            cursor.moveToFirst();
            assertEquals("large.bin", cursor.getString(cursor.getColumnIndexOrThrow(
                    OpenableColumns.DISPLAY_NAME)));
            assertEquals(size, cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)));
        }

        long[] markerOffsets = {0, size / 2, size - PatternFileFactory.MARKER_SIZE};
        try (ParcelFileDescriptor descriptor = resolver.openFileDescriptor(uri, "r");
             FileInputStream input = new FileInputStream(descriptor.getFileDescriptor())) {
            FileChannel channel = input.getChannel();
            ByteBuffer oneByte = ByteBuffer.allocate(1);
            for (long offset : markerOffsets) {
                oneByte.clear();
                channel.position(offset);
                assertEquals(1, channel.read(oneByte));
                assertEquals(PatternFileFactory.expectedByte(offset), oneByte.array()[0]);
            }
        }
    }

    @Test
    public void pipeUri_supportsSequentialReadsForNonSeekableFailureTests() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Uri uri = StreamingTestContentProvider.uri("pipe", "pipe.bin", 32 * 1024L + 3);
        try (ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(uri, "r");
             FileInputStream input = new FileInputStream(descriptor.getFileDescriptor())) {
            byte[] buffer = new byte[4096];
            long position = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                for (int i = 0; i < count; i++) {
                    assertEquals(PatternFileFactory.expectedByte(position + i), buffer[i]);
                }
                position += count;
            }
            assertEquals(32 * 1024L + 3, position);
        }
    }

    @Test
    public void pipeUri_usesOpenableColumnsSizeInsteadOfReportingMissing() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        long expectedSize = 32 * 1024L + 3;
        Uri uri = StreamingTestContentProvider.uri("pipe", "metadata-pipe.bin", expectedSize);

        assertEquals(expectedSize,
                FileHelper.getFileSize(context, context.getContentResolver(), uri));
    }

    @Test
    public void missingAndDeniedUris_failDeterministically() throws Exception {
        ContentResolver resolver = InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getContentResolver();
        try {
            resolver.openFileDescriptor(
                    StreamingTestContentProvider.uri("missing", "gone.bin", 1), "r");
            fail("Missing fixture must throw");
        } catch (FileNotFoundException expected) {
            // Expected.
        }

        try {
            resolver.openFileDescriptor(
                    StreamingTestContentProvider.uri("denied", "private.bin", 1), "r");
            fail("Denied fixture must throw");
        } catch (SecurityException expected) {
            // Expected.
        }
    }
}
