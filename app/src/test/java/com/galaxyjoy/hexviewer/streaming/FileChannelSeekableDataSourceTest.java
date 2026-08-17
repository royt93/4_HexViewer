package com.galaxyjoy.hexviewer.streaming;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileChannelSeekableDataSourceTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void performsPositionalReadsWithoutMovingASharedCursor() throws Exception {
        File file = temporaryFolder.newFile("source.bin");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[]{0, 1, 2, 3, 4, 5});
        }

        try (FileChannelSeekableDataSource source =
                     FileChannelSeekableDataSource.open(file)) {
            byte[] second = new byte[2];
            byte[] first = new byte[2];
            assertEquals(2, source.readAt(4, second, 0, 2));
            assertEquals(2, source.readAt(1, first, 0, 2));
            assertArrayEquals(new byte[]{4, 5}, second);
            assertArrayEquals(new byte[]{1, 2}, first);
            assertEquals(6, source.size());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativePosition() throws Exception {
        File file = temporaryFolder.newFile("negative.bin");
        try (FileChannelSeekableDataSource source =
                     FileChannelSeekableDataSource.open(file)) {
            source.readAt(-1, new byte[1], 0, 1);
        }
    }

    @Test
    public void readAtOrPastEofReturnsMinusOne() throws Exception {
        File file = temporaryFolder.newFile("eof.bin");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[]{1, 2, 3});
        }

        try (FileChannelSeekableDataSource source = FileChannelSeekableDataSource.open(file)) {
            byte[] destination = new byte[4];
            assertEquals(-1, source.readAt(3, destination, 0, destination.length));
            assertEquals(-1, source.readAt(100, destination, 0, destination.length));
        }
    }

    @Test
    public void zeroLengthReadReturnsZeroWithoutTouchingChannel() throws Exception {
        File file = temporaryFolder.newFile("zero.bin");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[]{1, 2, 3});
        }

        try (FileChannelSeekableDataSource source = FileChannelSeekableDataSource.open(file)) {
            assertEquals(0, source.readAt(0, new byte[0], 0, 0));
            // Also valid at an offset beyond EOF: a zero-length read must not throw.
            assertEquals(0, source.readAt(999, new byte[1], 0, 0));
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void rejectsLengthLargerThanDestinationBuffer() throws Exception {
        File file = temporaryFolder.newFile("bounds.bin");
        try (FileChannelSeekableDataSource source = FileChannelSeekableDataSource.open(file)) {
            source.readAt(0, new byte[2], 0, 3);
        }
    }

    @Test(expected = IOException.class)
    public void readAfterCloseThrows() throws Exception {
        File file = temporaryFolder.newFile("closed.bin");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[]{1, 2, 3});
        }

        FileChannelSeekableDataSource source = FileChannelSeekableDataSource.open(file);
        source.close();
        source.readAt(0, new byte[1], 0, 1);
    }

    @Test
    public void closeDelegatesToProvidedOwnerInsteadOfChannelDirectly() throws Exception {
        File file = temporaryFolder.newFile("owner.bin");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[]{1});
        }
        java.io.FileInputStream stream = new java.io.FileInputStream(file);
        final boolean[] ownerClosed = {false};
        java.io.Closeable owner = () -> {
            ownerClosed[0] = true;
            stream.close();
        };
        FileChannelSeekableDataSource source =
                new FileChannelSeekableDataSource(stream.getChannel(), owner);

        source.close();

        assertTrue("close() must delegate to the supplied owner, not just the channel",
                ownerClosed[0]);
    }
}
