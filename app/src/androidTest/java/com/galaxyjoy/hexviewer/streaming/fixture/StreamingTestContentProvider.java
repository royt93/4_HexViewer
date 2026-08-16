package com.galaxyjoy.hexviewer.streaming.fixture;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only provider exposing seekable, pipe, denied, and missing content URIs.
 *
 * URI examples:
 * content://com.galaxyjoy.hexviewer.test.streaming/seekable/data.bin?size=1024
 * content://com.galaxyjoy.hexviewer.test.streaming/pipe/data.bin?size=1024
 */
public final class StreamingTestContentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.galaxyjoy.hexviewer.test.streaming";
    private static final long DEFAULT_SIZE = 4 * 1024L;
    private static final ConcurrentHashMap<String, AtomicInteger> WRITE_OPEN_COUNTS =
            new ConcurrentHashMap<>();

    public static Uri uri(String mode, String name, long size) {
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(mode)
                .appendPath(name)
                .appendQueryParameter("size", Long.toString(size))
                .build();
    }

    public static boolean backingFileExists(@NonNull android.content.Context context, Uri uri) {
        return backingFile(context, uri).exists();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder
    ) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE
        });
        String fixtureMode = firstPathSegmentUnchecked(uri);
        Long size = "unknown".equals(fixtureMode) ? null : requestedSize(uri);
        if (isWritableMode(fixtureMode)) {
            File file = backingFile(getContext(), uri);
            if (file.exists()) size = file.length();
        }
        cursor.addRow(new Object[]{fileName(uri), size});
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "application/octet-stream";
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        String fixtureMode = firstPathSegment(uri);
        if ("denied".equals(fixtureMode)) {
            throw new SecurityException("Permission revoked by streaming test provider");
        }
        if ("missing".equals(fixtureMode)) {
            throw new FileNotFoundException("Missing streaming test fixture");
        }
        if ("pipe".equals(fixtureMode) || "unknown".equals(fixtureMode)
                || "slowpipe".equals(fixtureMode)) {
            final boolean slow = "slowpipe".equals(fixtureMode);
            return openPipeHelper(uri, "application/octet-stream", null, requestedSize(uri),
                    (output, pipeUri, mimeType, opts, size) -> {
                        try (FileOutputStream stream = new FileOutputStream(output.getFileDescriptor())) {
                            byte[] buffer = new byte[8 * 1024];
                            byte[] tailMarker = "late-target".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                            long markerStart = Math.max(0L, size - tailMarker.length);
                            long position = 0;
                            while (position < size) {
                                int count = (int) Math.min(buffer.length, size - position);
                                for (int i = 0; i < count; i++) {
                                    long absolute = position + i;
                                    if (slow && absolute >= markerStart) {
                                        buffer[i] = tailMarker[(int) (absolute - markerStart)];
                                    } else {
                                        buffer[i] = PatternFileFactory.expectedByte(absolute);
                                    }
                                }
                                stream.write(buffer, 0, count);
                                position += count;
                                if (slow) {
                                    try {
                                        Thread.sleep(1L);
                                    } catch (InterruptedException interrupted) {
                                        Thread.currentThread().interrupt();
                                        return;
                                    }
                                }
                            }
                        } catch (IOException ignored) {
                            // Closing the read side early is an expected cancellation scenario.
                        }
                    });
        }
        if (isWritableMode(fixtureMode)) {
            File file = backingFile(getContext(), uri);
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Unable to create fixture directory");
                }
                if (!file.exists() && !file.createNewFile()) {
                    throw new IOException("Unable to create fixture file");
                }
                boolean writing = mode.indexOf('w') >= 0 || mode.indexOf('t') >= 0;
                if (writing && "failsecondwrite".equals(fixtureMode)) {
                    int open = WRITE_OPEN_COUNTS.computeIfAbsent(uri.toString(),
                            ignored -> new AtomicInteger()).incrementAndGet();
                    if (open == 2) {
                        throw new IOException("Injected provider destination write failure");
                    }
                }
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode));
            } catch (IOException exception) {
                FileNotFoundException wrapped = new FileNotFoundException(exception.getMessage());
                wrapped.initCause(exception);
                throw wrapped;
            }
        }
        if (!"seekable".equals(fixtureMode)) {
            throw new FileNotFoundException("Unknown fixture mode: " + fixtureMode);
        }

        try {
            long size = requestedSize(uri);
            File file = PatternFileFactory.createSparsePatternFile(
                    getContext(),
                    "provider_" + Math.abs(uri.toString().hashCode()) + ".bin",
                    size,
                    markerOffsets(size)
            );
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (IOException | IllegalArgumentException exception) {
            FileNotFoundException wrapped = new FileNotFoundException(exception.getMessage());
            wrapped.initCause(exception);
            throw wrapped;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] args) {
        if (!isWritableMode(firstPathSegmentUnchecked(uri))) return 0;
        File file = backingFile(getContext(), uri);
        return !file.exists() || file.delete() ? 1 : 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] args) {
        return 0;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    private static long[] markerOffsets(long size) {
        if (size <= 0) {
            return new long[0];
        }
        long tail = Math.max(0, size - PatternFileFactory.MARKER_SIZE);
        long middle = size / 2;
        if (middle == 0 || middle == tail) {
            return new long[]{0};
        }
        if (tail == 0) {
            return new long[]{0, middle};
        }
        return new long[]{0, middle, tail};
    }

    private static String firstPathSegment(Uri uri) throws FileNotFoundException {
        if (uri.getPathSegments().isEmpty()) {
            throw new FileNotFoundException("Fixture mode is missing");
        }
        return uri.getPathSegments().get(0);
    }

    private static String firstPathSegmentUnchecked(Uri uri) {
        return uri.getPathSegments().isEmpty() ? "" : uri.getPathSegments().get(0);
    }

    private static String fileName(Uri uri) {
        if (uri.getPathSegments().size() < 2) {
            return "fixture.bin";
        }
        return uri.getLastPathSegment();
    }

    private static long requestedSize(Uri uri) {
        String value = uri.getQueryParameter("size");
        if (value == null) {
            return DEFAULT_SIZE;
        }
        try {
            long size = Long.parseLong(value);
            return Math.max(0, size);
        } catch (NumberFormatException ignored) {
            return DEFAULT_SIZE;
        }
    }

    private static File backingFile(android.content.Context context, Uri uri) {
        return new File(context.getCacheDir(),
                "provider_writable_" + Integer.toHexString(uri.toString().hashCode()) + ".bin");
    }

    private static boolean isWritableMode(String mode) {
        return "writable".equals(mode) || "failsecondwrite".equals(mode);
    }

}
