package com.galaxyjoy.hexviewer.streaming;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;

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
}
