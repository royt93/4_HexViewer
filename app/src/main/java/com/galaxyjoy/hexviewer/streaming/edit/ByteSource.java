package com.galaxyjoy.hexviewer.streaming.edit;

import java.io.IOException;

/** Random-access source used by the streaming edit model. */
@FunctionalInterface
public interface ByteSource {
    /**
     * Reads exactly {@code length} bytes starting at {@code absoluteOffset}.
     * Implementations must throw when the requested range cannot be read fully.
     */
    byte[] read(long absoluteOffset, int length) throws IOException;
}
