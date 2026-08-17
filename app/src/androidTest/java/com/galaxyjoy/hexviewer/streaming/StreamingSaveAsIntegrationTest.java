package com.galaxyjoy.hexviewer.streaming;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.galaxyjoy.hexviewer.constants.AppConstants;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.streaming.fixture.PatternFileFactory;
import com.galaxyjoy.hexviewer.streaming.fixture.StreamingTestContentProvider;
import com.galaxyjoy.hexviewer.ui.act.ActMain;
import com.galaxyjoy.hexviewer.ui.task.TaskSave;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** On-device proof for the complete provider-backed streaming Save As transaction. */
@RunWith(AndroidJUnit4.class)
public class StreamingSaveAsIntegrationTest {
    private static final long LARGE_SIZE = AppConstants.MAX_NORMAL_FILE_SIZE + 1L;

    @Test
    public void largeStreamingSaveAs_appliesEditAndPreservesFullSizeAndMarkers() throws Exception {
        Uri source = StreamingTestContentProvider.uri("seekable", "save-as-source.bin", LARGE_SIZE);
        Uri destination = StreamingTestContentProvider.uri("writable", "save-as-destination.bin", 0);
        Uri staging = StreamingTestContentProvider.uri("writable", "save-as-stage.bin", 0);

        try (ActivityScenario<ActMain> scenario = openStreamingSource(source)) {
            scenario.onActivity(activity -> {
                LineEntry first = activity.getPayloadHex().getAdapter().getItem(0);
                assertNotNull(first);
                first.getRaw().set(0, (byte) 0xA5);
                first.setUpdated(true);
            });

            SaveOutcome outcome = save(scenario, destination, staging, null, true);
            assertTrue("Streaming Save As should succeed", outcome.success);

            scenario.onActivity(activity -> {
                ContentResolver resolver = activity.getContentResolver();
                assertEquals(LARGE_SIZE, statSize(resolver, destination));
                assertEquals((byte) 0xA5, readByte(resolver, destination, 0));
                assertEquals(PatternFileFactory.expectedByte(LARGE_SIZE / 2),
                        readByte(resolver, destination, LARGE_SIZE / 2));
                assertEquals(PatternFileFactory.expectedByte(LARGE_SIZE - 1),
                        readByte(resolver, destination, LARGE_SIZE - 1));
                assertFalse("Staging document must be removed after commit",
                        StreamingTestContentProvider.backingFileExists(activity, staging));
            });
        }
    }

    @Test
    public void overwriteProviderFailure_restoresExactOriginalAndCleansTemps() throws Exception {
        Uri source = StreamingTestContentProvider.uri("seekable", "rollback-source.bin", LARGE_SIZE);
        Uri destination = StreamingTestContentProvider.uri("failsecondwrite", "rollback-destination.bin", 0);
        Uri staging = StreamingTestContentProvider.uri("writable", "rollback-stage.bin", 0);
        Uri backup = StreamingTestContentProvider.uri("writable", "rollback-backup.bin", 0);
        byte[] original = new byte[384 * 1024 + 17];
        for (int i = 0; i < original.length; i++) original[i] = (byte) (i * 37 + 11);

        try (ActivityScenario<ActMain> scenario = openStreamingSource(source)) {
            scenario.onActivity(activity -> writeAll(activity.getContentResolver(), destination, original));
            final byte[][] beforeDigest = new byte[1][];
            scenario.onActivity(activity -> beforeDigest[0] = digest(activity.getContentResolver(), destination));
            SaveOutcome outcome = save(scenario, destination, staging, backup, false);
            assertFalse("Injected provider failure must be reported", outcome.success);

            scenario.onActivity(activity -> {
                ContentResolver resolver = activity.getContentResolver();
                assertEquals(original.length, statSize(resolver, destination));
                assertArrayEquals("Rollback must restore the exact original destination",
                        beforeDigest[0], digest(resolver, destination));
                assertFalse(StreamingTestContentProvider.backingFileExists(activity, staging));
                assertFalse(StreamingTestContentProvider.backingFileExists(activity, backup));
            });
        }
    }

    @Test
    public void newDestinationStageFailure_deletesIncompleteDestination() throws Exception {
        Uri source = StreamingTestContentProvider.uri("seekable", "cleanup-source.bin", LARGE_SIZE);
        Uri destination = StreamingTestContentProvider.uri("writable", "cleanup-destination.bin", 0);
        Uri deniedStage = StreamingTestContentProvider.uri("denied", "cleanup-stage.bin", 0);

        try (ActivityScenario<ActMain> scenario = openStreamingSource(source)) {
            scenario.onActivity(activity -> writeAll(activity.getContentResolver(), destination,
                    new byte[]{1, 2, 3, 4}));
            SaveOutcome outcome = save(scenario, destination, deniedStage, null, true);
            assertFalse("Denied staging provider must fail Save As", outcome.success);
            scenario.onActivity(activity -> assertFalse(
                    "A newly-created failed destination must be deleted",
                    StreamingTestContentProvider.backingFileExists(activity, destination)));
        }
    }

    /**
     * Regression test: an unknown-size content:// pipe that resolves to exactly 0 bytes must
     * still be a valid Save As source. Today TaskSave.saveStreamingCopy() builds a single
     * whole-window DirtyRange out of the resident window's bytes; for a genuinely empty
     * window (original.length == replacement.length == 0) the DirtyRange constructor throws
     * IllegalArgumentException("dirty ranges must be non-empty and fixed-length"), which
     * doInBackground() reports as a generic save failure instead of writing an empty
     * destination file. See DirtyRangeTest#constructor_rejectsEmptyRanges for the underlying
     * validation that trips this up.
     */
    @Test
    public void emptyUnknownSizeStreamingSource_saveAsShouldSucceedWithEmptyDestination()
            throws Exception {
        Uri source = StreamingTestContentProvider.uri("unknown", "empty-source.bin", 0);
        Uri destination = StreamingTestContentProvider.uri("writable", "empty-destination.bin", 0);
        Uri staging = StreamingTestContentProvider.uri("writable", "empty-stage.bin", 0);

        try (ActivityScenario<ActMain> scenario = openEmptyUnknownSizeStreamingSource(source)) {
            SaveOutcome outcome = save(scenario, destination, staging, null, true);

            assertTrue("Save As of a genuinely empty streaming window should succeed, not fail "
                    + "with a DirtyRange 'non-empty and fixed-length' error", outcome.success);
            scenario.onActivity(activity -> assertEquals(0L,
                    statSize(activity.getContentResolver(), destination)));
        }
    }

    /**
     * Regression test: TaskSave.saveStreamingCopy() used to re-read "original" bytes from the
     * source right before the copy started, so it only ever caught an external write racing the
     * copy itself. An external write that lands between window-load time and the moment the user
     * taps Save (the whole duration they were editing) went undetected and would have been
     * silently baked into the destination as if it were the pre-edit content. FileData now
     * captures a window-load-time snapshot (see FileData#setStreamingWindowOriginal) that
     * TaskSave compares against instead, so this must now be caught as a conflict.
     */
    @Test
    public void externalWriteDuringEditSession_beforeSaveClicked_isDetectedAsConflict() throws Exception {
        long size = LARGE_SIZE;
        Uri source = StreamingTestContentProvider.uri("writable", "conflict-source.bin", 0);
        Uri destination = StreamingTestContentProvider.uri("writable", "conflict-destination.bin", 0);
        Uri staging = StreamingTestContentProvider.uri("writable", "conflict-stage.bin", 0);
        ContentResolver bootstrapResolver =
                ApplicationProvider.<android.content.Context>getApplicationContext().getContentResolver();
        writePattern(bootstrapResolver, source, size, -1L, (byte) 0);

        try (ActivityScenario<ActMain> scenario = openStreamingSource(source)) {
            // The window (and its original-bytes snapshot) is now loaded. Simulate another
            // process/app modifying the source file while the user is still editing, i.e. well
            // before Save is tapped.
            scenario.onActivity(activity ->
                    writePattern(activity.getContentResolver(), source, size, 3L, (byte) 0x7E));

            SaveOutcome outcome = save(scenario, destination, staging, null, true);
            assertFalse("An external write made during the edit session (before Save was tapped) "
                    + "must be detected as a conflict, not silently overwritten", outcome.success);
        }
    }

    private static void writePattern(ContentResolver resolver, Uri uri, long size,
                                     long overrideOffset, byte overrideValue) {
        try (OutputStream output = resolver.openOutputStream(uri, "wt")) {
            assertNotNull(output);
            byte[] buffer = new byte[64 * 1024];
            long position = 0;
            while (position < size) {
                int count = (int) Math.min(buffer.length, size - position);
                for (int i = 0; i < count; i++) {
                    long absolute = position + i;
                    buffer[i] = absolute == overrideOffset
                            ? overrideValue : PatternFileFactory.expectedByte(absolute);
                }
                output.write(buffer, 0, count);
                position += count;
            }
        } catch (Exception failure) {
            throw new AssertionError("Unable to write provider fixture pattern", failure);
        }
    }

    private static ActivityScenario<ActMain> openEmptyUnknownSizeStreamingSource(Uri source) {
        ActivityScenario<ActMain> scenario = ActivityScenario.launch(ActMain.class);
        scenario.onActivity(activity -> activity.getLauncherOpen().processFileOpen(
                new FileData(activity, source, false), null, false));
        AtomicBoolean loaded = new AtomicBoolean(false);
        long deadline = SystemClock.elapsedRealtime() + 15_000L;
        while (!loaded.get() && SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> {
                FileData data = activity.getFileData();
                loaded.set(data != null && data.isStreaming() && data.getRealSize() == 0L
                        && !data.isSizeUnknown());
            });
            if (!loaded.get()) SystemClock.sleep(50L);
        }
        assertTrue("Timed out opening empty unknown-size streaming source", loaded.get());
        return scenario;
    }

    private static ActivityScenario<ActMain> openStreamingSource(Uri source) {
        ActivityScenario<ActMain> scenario = ActivityScenario.launch(ActMain.class);
        scenario.onActivity(activity -> activity.getLauncherOpen().processFileOpen(
                new FileData(activity, source, false), null, false));
        AtomicBoolean loaded = new AtomicBoolean(false);
        long deadline = SystemClock.elapsedRealtime() + 15_000L;
        while (!loaded.get() && SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> {
                FileData data = activity.getFileData();
                loaded.set(data != null && data.isStreaming()
                        && data.getRealSize() == LARGE_SIZE
                        && activity.getPayloadHex().getAdapter().getCount() > 0);
            });
            if (!loaded.get()) SystemClock.sleep(50L);
        }
        assertTrue("Timed out opening streaming source", loaded.get());
        return scenario;
    }

    private static SaveOutcome save(ActivityScenario<ActMain> scenario, Uri destination,
                                    Uri staging, Uri backup, boolean destinationCreated)
            throws InterruptedException {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        scenario.onActivity(activity -> {
            FileData source = activity.getFileData();
            FileData target = new FileData(activity, destination, false);
            TaskSave.Request request = new TaskSave.Request(target,
                    activity.getPayloadHex().getAdapter().getEntries().getItems(), null,
                    source, staging, backup, destinationCreated);
            new TaskSave(activity, (fd, saved, runnable) -> {
                success.set(saved);
                finished.countDown();
            }).execute(request);
        });
        assertTrue("Timed out waiting for streaming Save As",
                finished.await(20, TimeUnit.SECONDS));
        return new SaveOutcome(success.get());
    }

    private static void writeAll(ContentResolver resolver, Uri uri, byte[] bytes) {
        try (OutputStream output = resolver.openOutputStream(uri, "wt")) {
            assertNotNull(output);
            output.write(bytes);
        } catch (Exception failure) {
            throw new AssertionError("Unable to initialize provider fixture", failure);
        }
    }

    private static long statSize(ContentResolver resolver, Uri uri) {
        try (ParcelFileDescriptor descriptor = resolver.openFileDescriptor(uri, "r")) {
            assertNotNull(descriptor);
            return descriptor.getStatSize();
        } catch (Exception failure) {
            throw new AssertionError("Unable to stat provider fixture", failure);
        }
    }

    private static byte readByte(ContentResolver resolver, Uri uri, long offset) {
        try (ParcelFileDescriptor descriptor = resolver.openFileDescriptor(uri, "r");
             FileInputStream input = new FileInputStream(descriptor.getFileDescriptor())) {
            input.getChannel().position(offset);
            int value = input.read();
            assertTrue("Unexpected EOF at " + offset, value >= 0);
            return (byte) value;
        } catch (Exception failure) {
            throw new AssertionError("Unable to read provider fixture", failure);
        }
    }

    private static byte[] digest(ContentResolver resolver, Uri uri) {
        try (InputStream input = resolver.openInputStream(uri)) {
            assertNotNull(input);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) digest.update(buffer, 0, count);
            }
            return digest.digest();
        } catch (Exception failure) {
            throw new AssertionError("Unable to digest provider fixture", failure);
        }
    }

    private static final class SaveOutcome {
        final boolean success;

        SaveOutcome(boolean success) {
            this.success = success;
        }
    }
}
