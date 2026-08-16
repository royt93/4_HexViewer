package com.galaxyjoy.hexviewer.streaming;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

/** Random-access source for a seekable Android {@code content://} URI. */
public final class ContentUriSeekableDataSource implements SeekableDataSource {
    private final ParcelFileDescriptor descriptor;
    private final FileInputStream stream;
    private final FileChannel channel;
    private final long size;
    private boolean closed;

    public static ContentUriSeekableDataSource open(ContentResolver resolver, Uri uri) throws IOException {
        Objects.requireNonNull(resolver, "resolver");
        Objects.requireNonNull(uri, "uri");
        ParcelFileDescriptor descriptor = resolver.openFileDescriptor(uri, "r");
        if (descriptor == null) {
            throw new FileNotFoundException("Provider returned no file descriptor for " + uri);
        }
        return new ContentUriSeekableDataSource(descriptor);
    }

    ContentUriSeekableDataSource(ParcelFileDescriptor descriptor) throws IOException {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        FileInputStream openedStream = null;
        try {
            openedStream = new FileInputStream(descriptor.getFileDescriptor());
            FileChannel openedChannel = openedStream.getChannel();
            // position() is non-blocking and fails with ESPIPE for pipe-backed providers.
            long originalPosition = openedChannel.position();
            openedChannel.position(originalPosition);
            long statSize = descriptor.getStatSize();
            this.size = statSize >= 0 ? statSize : openedChannel.size();
            this.stream = openedStream;
            this.channel = openedChannel;
        } catch (IOException | RuntimeException error) {
            if (openedStream != null) {
                try {
                    openedStream.close();
                } catch (IOException closeError) {
                    error.addSuppressed(closeError);
                }
            }
            try {
                descriptor.close();
            } catch (IOException closeError) {
                error.addSuppressed(closeError);
            }
            if (error instanceof IOException) {
                throw (IOException) error;
            }
            throw error;
        }
    }

    @Override
    public long size() throws IOException {
        ensureOpen();
        return size;
    }

    @Override
    public int readAt(long position, byte[] destination, int offset, int length) throws IOException {
        FileChannelSeekableDataSource.validateRead(position, destination, offset, length);
        ensureOpen();
        if (length == 0) {
            return 0;
        }
        return channel.read(ByteBuffer.wrap(destination, offset, length), position);
    }

    private void ensureOpen() throws IOException {
        if (closed || !channel.isOpen()) {
            throw new IOException("Data source is closed");
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        IOException failure = null;
        try {
            stream.close();
        } catch (IOException error) {
            failure = error;
        }
        try {
            descriptor.close();
        } catch (IOException error) {
            if (failure == null) {
                failure = error;
            } else {
                failure.addSuppressed(error);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
