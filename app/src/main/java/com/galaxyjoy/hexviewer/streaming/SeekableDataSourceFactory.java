package com.galaxyjoy.hexviewer.streaming;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Opens seekable content directly and falls back to a private cache spool when required. */
public final class SeekableDataSourceFactory {
    private static final PersistentSpoolCache SPOOL_CACHE = new PersistentSpoolCache();
    private SeekableDataSourceFactory() {
    }

    public static SeekableDataSource openContentUri(
            ContentResolver resolver,
            Uri uri,
            File cacheDirectory,
            long expectedSize) throws IOException {
        return openContentUri(resolver, uri, cacheDirectory, expectedSize, null, null);
    }

    public static SeekableDataSource openContentUri(
            ContentResolver resolver,
            Uri uri,
            File cacheDirectory,
            long expectedSize,
            SpoolingSeekableDataSource.ProgressListener progressListener,
            SpoolingSeekableDataSource.CancellationChecker cancellationChecker) throws IOException {
        Objects.requireNonNull(resolver, "resolver");
        Objects.requireNonNull(uri, "uri");
        final long normalizedExpectedSize = expectedSize < 0L ? -1L : expectedSize;
        final String cacheKey = uri.toString();
        SeekableDataSource retained = SPOOL_CACHE.acquireExisting(
                cacheKey, normalizedExpectedSize);
        if (retained != null) return retained;
        try {
            return ContentUriSeekableDataSource.open(resolver, uri);
        } catch (IOException directFailure) {
            try {
                return SPOOL_CACHE.acquire(cacheKey, normalizedExpectedSize, () -> {
                    InputStream input = resolver.openInputStream(uri);
                    if (input == null) {
                        throw new IOException("Provider returned no input stream for " + uri);
                    }
                    SpoolingSeekableDataSource spooled =
                            SpoolingSeekableDataSource.spool(input, cacheDirectory,
                                    normalizedExpectedSize,
                                    progressListener, cancellationChecker);
                    return spooled.detachCacheFile();
                });
            } catch (IOException spoolFailure) {
                spoolFailure.addSuppressed(directFailure);
                throw spoolFailure;
            }
        }
    }

    /** Releases a retained non-seekable cache once the owning file is closed/replaced. */
    public static void releaseContentUri(Uri uri) {
        if (uri != null) SPOOL_CACHE.release(uri.toString());
    }

    /** Clears every idle retained spool. Intended for application memory/storage cleanup. */
    public static void clearRetainedSpools() {
        SPOOL_CACHE.clear();
    }
}
