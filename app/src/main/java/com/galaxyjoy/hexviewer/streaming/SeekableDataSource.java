package com.galaxyjoy.hexviewer.streaming;

import java.io.Closeable;
import java.io.IOException;

/** Random-access byte source used by the streaming viewer. */
public interface SeekableDataSource extends Closeable {
    /** Returns the source size, or {@code -1} when it is not known yet. */
    long size() throws IOException;

    /**
     * Reads bytes beginning at an absolute source offset.
     *
     * @return number of bytes read, or {@code -1} when {@code position} is at EOF
     */
    int readAt(long position, byte[] destination, int offset, int length) throws IOException;
}
