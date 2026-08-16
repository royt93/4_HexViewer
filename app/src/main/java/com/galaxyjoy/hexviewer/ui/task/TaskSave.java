/**
 *******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Task used to save a file.
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.task;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;


import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.util.UIHelper;
import com.galaxyjoy.hexviewer.util.SysHelper;
import com.galaxyjoy.hexviewer.util.io.RandomAccessFileChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

public class TaskSave extends ProgressTask<ContentResolver, TaskSave.Request, TaskSave.Result> {
    private static final int MAX_LENGTH = SysHelper.MAX_BY_ROW_16 * 10000;
    private RandomAccessFileChannel mRandomAccessFileChannel = null;
    private final SaveResultListener mListener;
    private final ContentResolver mContentResolver;
    private final Context mContext;

    public static class Result {
        private Runnable runnable;
        private String exception = null;
        private FileData fd = null;
    }

    public static class Request {
        private final FileData mFd;
        private final List<LineEntry> mEntries;
        private final Runnable mRunnable;
        private final FileData mStreamingSource;
        private final Uri mStagingUri;
        private final Uri mBackupUri;
        private final boolean mDestinationCreated;

        public Request(FileData fd,
                       List<LineEntry> entries,
                       final Runnable runnable) {
            mFd = fd;
            mEntries = entries;
            mRunnable = runnable;
            mStreamingSource = null;
            mStagingUri = null;
            mBackupUri = null;
            mDestinationCreated = false;
        }

        /** Transactional Save As request. Staging and optional backup are disposable files. */
        public Request(FileData destination, List<LineEntry> entries,
                       final Runnable runnable, FileData streamingSource,
                       Uri stagingUri, Uri backupUri, boolean destinationCreated) {
            mFd = destination;
            mEntries = entries;
            mRunnable = runnable;
            mStreamingSource = streamingSource;
            mStagingUri = stagingUri;
            mBackupUri = backupUri;
            mDestinationCreated = destinationCreated;
        }

    }

    public interface SaveResultListener {
        void onSaveResult(FileData fd,
                          boolean success,
                          final Runnable userRunnable);
    }

    public TaskSave(final Activity activity,
                    final SaveResultListener listener) {
        super(activity, false);
        mContentResolver = activity.getContentResolver();
        mContext = activity;
        mListener = listener;
    }

    /**
     * Called before the execution of the task.
     *
     * @return The Config.
     */
    @Override
    public ContentResolver onPreExecute() {
        super.onPreExecute();
        return mContentResolver;
    }

    /**
     * Called after the execution of the task.
     *
     * @param result The result.
     */
    @Override
    public void onPostExecute(final Result result) {
        super.onPostExecute(result);
        if (isCancelled()) {
            // Never delete result.fd here: for in-place and streaming saves it is
            // the user's source file, not a disposable temporary destination.
            UIHelper.showErrorDialog(mContext,
                    R.string.error_title,
                    mContext.getString(R.string.operation_canceled));
        } else if (result.exception == null) UIHelper.toast(mContext, mContext.getString(R.string.save_success));
        else
            UIHelper.showErrorDialog(mContext,
                    R.string.error_title,
                    mContext.getString(R.string.exception) + ": " + result.exception);
        if (mListener != null)
            mListener.onSaveResult(result.fd,
                    result.exception == null && !isCancelled(),
                    result.runnable);
    }

    /**
     * Closes the stream.
     */
    private void close() {
        if (mRandomAccessFileChannel != null) {
            mRandomAccessFileChannel.close();
            mRandomAccessFileChannel = null;
        }
    }

    /**
     * Called when the async task is cancelled.
     */
    @Override
    public void onCancelled() {
        super.onCancelled();
        close();
        UIHelper.toast(mContext, mContext.getString(R.string.operation_canceled));
    }

    /**
     * Performs a computation on a background thread.
     *
     * @param contentResolver ContentResolver.
     * @param request         Request.
     * @return The result.
     */
    @Override
    public Result doInBackground(final ContentResolver contentResolver, final Request request) {
        final Result result = new Result();
        if (request == null) {
            result.exception = "Invalid param!";
            return result;
        }
        result.fd = request.mFd;
        result.runnable = request.mRunnable;
        publishProgress(0L);
        try {
            if (request.mStreamingSource != null) {
                saveStreamingCopy(contentResolver, request);
                return result;
            }
            final byte[] data = collectBytes(request.mEntries);
            if (!isCancelled()) {
                validateWindowLength(result.fd.isStreaming(), result.fd.isSequential(),
                        result.fd.getSize(), data.length);
                mTotalSize = data.length;
                if (result.fd.isSequential()) {
                    saveFixedLengthInPlace(contentResolver, result.fd, data);
                    return result;
                }
                mRandomAccessFileChannel = RandomAccessFileChannel.openForWriteOnly(
                        contentResolver, result.fd.getUri(), true);
                int maxLength = MAX_LENGTH;
                AtomicLong position = new AtomicLong(result.fd.getStartOffset());
                mRandomAccessFileChannel.setPosition(position.get());
                final long count = mTotalSize / maxLength;
                final long remain = mTotalSize - (count * maxLength);

                long offset = 0;
                for (long i = 0; i < count && !isCancelled(); i++) {
                    mRandomAccessFileChannel.write(data, (int) offset, maxLength);
                    publishProgress((long) maxLength);
                    offset += maxLength;
                }
                if (!isCancelled() && remain > 0) {
                    mRandomAccessFileChannel.write(data, (int) offset, (int) remain);
                    publishProgress(remain);
                }
            }
        } catch (final Exception e) {
            Log.e(TaskSave.class.getSimpleName(), "Save failed", e);
            result.exception = failureMessage(e);
        } finally {
            close();
        }
        return result;
    }

    private void saveStreamingCopy(ContentResolver resolver, Request request) throws IOException {
        if (request.mStagingUri == null) {
            throw new IOException("Transactional staging file is required for streaming Save As");
        }
        FileData source = request.mStreamingSource;
        byte[] replacement = collectBytes(request.mEntries);
        if (replacement.length != source.getSize()) {
            throw new IOException("Resident streaming window size changed");
        }
        byte[] original;
        try (com.galaxyjoy.hexviewer.streaming.SeekableDataSource dataSource =
                     com.galaxyjoy.hexviewer.streaming.SeekableDataSourceFactory.openContentUri(
                             resolver, source.getUri(), mContext.getCacheDir(), source.getRealSize())) {
            original = new byte[replacement.length];
            readFullyAt(dataSource, source.getStartOffset(), original);
        }
        com.galaxyjoy.hexviewer.streaming.edit.DirtyRange range =
                new com.galaxyjoy.hexviewer.streaming.edit.DirtyRange(
                        source.getStartOffset(), original, replacement);
        mTotalSize = source.getRealSize();
        boolean committed = false;
        try {
            try (InputStream input = resolver.openInputStream(source.getUri());
                 OutputStream output = resolver.openOutputStream(request.mStagingUri, "wt")) {
                if (input == null || output == null) throw new IOException("Unable to open Save As streams");
                com.galaxyjoy.hexviewer.streaming.edit.StreamingCopyHelper.copyAndApply(
                        input, output, source.getRealSize(), Collections.singletonList(range),
                        com.galaxyjoy.hexviewer.streaming.edit.StreamingCopyHelper.DEFAULT_BUFFER_SIZE,
                        this::isCancelled,
                        (copied, total) -> publishProgress(copied));
            }
            if (isCancelled()) throw new InterruptedIOException("Save As cancelled");
            commitStaging(resolver, request);
            committed = true;
        } finally {
            deleteUriQuietly(resolver, request.mStagingUri);
            deleteUriQuietly(resolver, request.mBackupUri);
            if (!committed && request.mDestinationCreated) {
                deleteUriQuietly(resolver, request.mFd.getUri());
            }
        }
    }

    private byte[] collectBytes(List<LineEntry> entries) {
        List<Byte> bytes = new ArrayList<>();
        for (LineEntry entry : entries) bytes.addAll(entry.getRaw());
        return SysHelper.toByteArray(bytes, mCancel);
    }

    private void saveFixedLengthInPlace(ContentResolver resolver, FileData fd, byte[] data)
            throws IOException {
        byte[] original = new byte[data.length];
        try (com.galaxyjoy.hexviewer.streaming.SeekableDataSource source =
                     com.galaxyjoy.hexviewer.streaming.SeekableDataSourceFactory.openContentUri(
                             resolver, fd.getUri(), mContext.getCacheDir(), fd.getRealSize())) {
            readFullyAt(source, fd.getStartOffset(), original);
        }
        try {
            writeFullyAt(resolver, fd.getUri(), fd.getStartOffset(), data, true);
        } catch (Exception failure) {
            try {
                writeFullyAt(resolver, fd.getUri(), fd.getStartOffset(), original, false);
            } catch (Exception rollback) {
                failure.addSuppressed(rollback);
            }
            rethrowSaveFailure(failure);
        }
    }

    private void commitStaging(ContentResolver resolver, Request request) throws IOException {
        transactionalCommit(request.mFd.getUri(), request.mStagingUri, request.mBackupUri,
                new TransactionStreams() {
                    @Override
                    public InputStream openInput(Uri uri) throws IOException {
                        InputStream input = resolver.openInputStream(uri);
                        if (input == null) throw new IOException("Unable to open transaction input");
                        return input;
                    }

                    @Override
                    public OutputStream openTruncatedOutput(Uri uri) throws IOException {
                        OutputStream output = resolver.openOutputStream(uri, "wt");
                        if (output == null) throw new IOException("Unable to open transaction output");
                        return output;
                    }
                }, this::isCancelled);
    }

    interface TransactionStreams {
        InputStream openInput(Uri uri) throws IOException;

        OutputStream openTruncatedOutput(Uri uri) throws IOException;
    }

    static void transactionalCommit(Uri destination, Uri staging, Uri backup,
                                    TransactionStreams streams, BooleanSupplier cancellation)
            throws IOException {
        boolean destinationTouched = false;
        try {
            if (backup != null) {
                copyStream(streams, destination, backup, false, cancellation);
            }
            destinationTouched = true;
            copyStream(streams, staging, destination, true, cancellation);
        } catch (Exception failure) {
            if (destinationTouched && backup != null) {
                try {
                    copyStream(streams, backup, destination, false, () -> false);
                } catch (Exception rollback) {
                    failure.addSuppressed(rollback);
                }
            }
            rethrowSaveFailure(failure);
        }
    }

    private static void copyStream(TransactionStreams streams, Uri inputUri, Uri outputUri,
                                   boolean cancellable, BooleanSupplier cancellation)
            throws IOException {
        try (InputStream input = streams.openInput(inputUri);
             OutputStream output = streams.openTruncatedOutput(outputUri)) {
            byte[] buffer = new byte[com.galaxyjoy.hexviewer.streaming.edit.StreamingCopyHelper.DEFAULT_BUFFER_SIZE];
            while (true) {
                if (cancellable && cancellation.getAsBoolean()) {
                    throw new InterruptedIOException("Save As cancelled");
                }
                int read = input.read(buffer);
                if (read < 0) break;
                if (read == 0) {
                    int value = input.read();
                    if (value < 0) break;
                    output.write(value);
                } else {
                    output.write(buffer, 0, read);
                }
            }
            output.flush();
        }
    }

    static void validateWindowLength(boolean streaming, boolean sequential,
                                     long expectedLength, int actualLength) throws IOException {
        if (streaming && !sequential) {
            throw new IOException("Streaming save has no resident window; reopen the destination");
        }
        if (sequential && actualLength != expectedLength) {
            throw new IOException("Fixed-length window changed from " + expectedLength
                    + " to " + actualLength + " bytes");
        }
    }

    static String failureMessage(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        if (message != null && !message.trim().isEmpty()) return message;
        return failure == null ? "Unknown save failure" : failure.getClass().getSimpleName();
    }

    private static void rethrowSaveFailure(Exception failure) throws IOException {
        if (failure instanceof IOException) throw (IOException) failure;
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        throw new IOException(failureMessage(failure), failure);
    }

    private void writeFullyAt(ContentResolver resolver, Uri uri, long position, byte[] data,
                              boolean cancellable) throws IOException {
        try (ParcelFileDescriptor descriptor = resolver.openFileDescriptor(uri, "rw")) {
            if (descriptor == null) throw new IOException("Unable to open destination descriptor");
            try (FileOutputStream output = new FileOutputStream(descriptor.getFileDescriptor())) {
                FileChannel channel = output.getChannel();
                channel.position(position);
                ByteBuffer buffer = ByteBuffer.wrap(data);
                int zeroWrites = 0;
                while (buffer.hasRemaining()) {
                    if (cancellable && isCancelled()) throw new InterruptedIOException("Save cancelled");
                    int written = channel.write(buffer);
                    if (written == 0) {
                        if (++zeroWrites >= 3) throw new IOException("Destination made no write progress");
                    } else {
                        zeroWrites = 0;
                        publishProgress((long) written);
                    }
                }
                channel.force(true);
            }
        }
    }

    private static void readFullyAt(com.galaxyjoy.hexviewer.streaming.SeekableDataSource source,
                                    long position, byte[] destination) throws IOException {
        int total = 0;
        int zeroReads = 0;
        while (total < destination.length) {
            int read = source.readAt(position + total, destination, total,
                    destination.length - total);
            if (read < 0) throw new IOException("Source ended before resident window");
            if (read == 0) {
                if (++zeroReads >= 3) throw new IOException("Source made no read progress");
            } else {
                total += read;
                zeroReads = 0;
            }
        }
    }

    private void deleteUriQuietly(ContentResolver resolver, Uri uri) {
        if (uri == null) return;
        try {
            resolver.delete(uri, null, null);
        } catch (Exception error) {
            Log.w(getClass().getSimpleName(), "Unable to delete temporary file: " + uri, error);
        }
    }
}
