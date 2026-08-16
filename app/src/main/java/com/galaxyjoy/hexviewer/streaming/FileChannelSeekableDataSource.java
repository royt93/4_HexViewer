package com.galaxyjoy.hexviewer.streaming;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

/** Seekable source backed by a {@link FileChannel}. */
public class FileChannelSeekableDataSource implements SeekableDataSource {
    private final FileChannel channel;
    private final Closeable owner;

    public static FileChannelSeekableDataSource open(File file) throws IOException {
        FileInputStream stream = new FileInputStream(Objects.requireNonNull(file, "file"));
        return new FileChannelSeekableDataSource(stream.getChannel(), stream);
    }

    public FileChannelSeekableDataSource(FileChannel channel, Closeable owner) {
        this.channel = Objects.requireNonNull(channel, "channel");
        this.owner = owner;
    }

    @Override
    public long size() throws IOException {
        ensureOpen();
        return channel.size();
    }

    @Override
    public int readAt(long position, byte[] destination, int offset, int length) throws IOException {
        validateRead(position, destination, offset, length);
        ensureOpen();
        if (length == 0) {
            return 0;
        }
        return channel.read(ByteBuffer.wrap(destination, offset, length), position);
    }

    protected static void validateRead(long position, byte[] destination, int offset, int length) {
        Objects.requireNonNull(destination, "destination");
        if (position < 0) {
            throw new IllegalArgumentException("position must be >= 0");
        }
        if (offset < 0 || length < 0 || offset > destination.length - length) {
            throw new IndexOutOfBoundsException("Invalid destination range");
        }
    }

    protected final FileChannel channel() {
        return channel;
    }

    private void ensureOpen() throws IOException {
        if (!channel.isOpen()) {
            throw new IOException("Data source is closed");
        }
    }

    @Override
    public void close() throws IOException {
        if (owner != null) {
            owner.close();
        } else {
            channel.close();
        }
    }
}
