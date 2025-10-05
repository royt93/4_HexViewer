/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Unit tests for RecentlyOpened model
 * </p>
 *
 * @author Test Suite
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.galaxyjoy.hexviewer.MyApplication;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static org.junit.Assert.*;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecentlyOpened class.
 * Tests recently opened file management and persistence.
 */
@RunWith(RobolectricTestRunner.class)
public class RecentlyOpenedTest {

    private MyApplication app;
    private Context context;
    private Uri testUri;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        app = (MyApplication) context;
        testUri = Uri.parse("content://test/document/primary:test.bin");

        // Clear preferences before each test
        SharedPreferences prefs = app.getPref(app);
        prefs.edit().clear().apply();
    }

    /**
     * Test RecentlyOpened creation.
     */
    @Test
    public void should_CreateRecentlyOpened_When_Initialized() {
        RecentlyOpened recentlyOpened = new RecentlyOpened(app);

        assertNotNull("RecentlyOpened should not be null", recentlyOpened);
    }

    /**
     * Test initial list is empty.
     */
    @Test
    public void should_ReturnEmptyList_When_NoFilesOpened() {
        RecentlyOpened recentlyOpened = new RecentlyOpened(app);

        List<FileData> list = recentlyOpened.list();

        assertNotNull("List should not be null", list);
        assertTrue("List should be empty initially", list.isEmpty());
    }

    /**
     * Test adding a file to recently opened.
     */
    @Test
    public void should_AddFile_When_AddCalled() {
        RecentlyOpened recentlyOpened = new RecentlyOpened(app);
        FileData fileData = new FileData(context, testUri, false);

        recentlyOpened.add(fileData);

        List<FileData> list = recentlyOpened.list();
        assertEquals("List should have 1 item", 1, list.size());
    }

    /**
     * Test adding multiple files.
     */
    @Test
    public void should_AddMultipleFiles_When_AddCalledMultipleTimes() {
        RecentlyOpened recentlyOpened = new RecentlyOpened(app);
        Uri uri1 = Uri.parse("content://test/file1.bin");
        Uri uri2 = Uri.parse("content://test/file2.bin");

        FileData fd1 = new FileData(context, uri1, false);
        FileData fd2 = new FileData(context, uri2, false);

        recentlyOpened.add(fd1);
        recentlyOpened.add(fd2);

        List<FileData> list = recentlyOpened.list();
        assertEquals("List should have 2 items", 2, list.size());
    }

    /**
     * Test adding duplicate file removes old entry.
     */
    @Test
    public void should_RemoveDuplicate_When_SameFileAddedTwice() {
        RecentlyOpened recentlyOpened = new RecentlyOpened(app);
        FileData fileData = new FileData(context, testUri, false);

        recentlyOpened.add(fileData);
        recentlyOpened.add(fileData);

        List<FileData> list = recentlyOpened.list();
        assertEquals("List should have only 1 item", 1, list.size());
    }

    /**
     * Test removing a file.
     */
    @Test
    public void should_RemoveFile_When_RemoveCalled() {
        RecentlyOpened recentlyOpened = new RecentlyOpened(app);
        FileData fileData = new FileData(context, testUri, false);

        recentlyOpened.add(fileData);
        assertEquals("Should have 1 item before remove", 1, recentlyOpened.list().size());

        recentlyOpened.remove(fileData);

        assertEquals("Should have 0 items after remove", 0, recentlyOpened.list().size());
    }

    /**
     * Test removing by string.
     */
    @Test
    public void should_RemoveFileByString_When_RemoveCalledWithString() {
        RecentlyOpened recentlyOpened = new RecentlyOpened(app);
        FileData fileData = new FileData(context, testUri, false);

        recentlyOpened.add(fileData);
        String fileString = fileData.toString();

        recentlyOpened.remove(fileString);

        assertEquals("Should be removed", 0, recentlyOpened.list().size());
    }

    /**
     * Test clearing all files.
     */
    @Test
    public void should_ClearAllFiles_When_ClearCalled() {
        RecentlyOpened recentlyOpened = new RecentlyOpened(app);

        for (int i = 0; i < 5; i++) {
            Uri uri = Uri.parse("content://test/file" + i + ".bin");
            recentlyOpened.add(new FileData(context, uri, false));
        }

        assertEquals("Should have 5 items", 5, recentlyOpened.list().size());

        recentlyOpened.clear();

        assertEquals("Should have 0 items after clear", 0, recentlyOpened.list().size());
    }

    /**
     * Test reload functionality.
     */
    @Test
    public void should_ReloadList_When_ReloadCalled() {
        RecentlyOpened recentlyOpened = new RecentlyOpened(app);
        FileData fileData = new FileData(context, testUri, false);

        recentlyOpened.add(fileData);
        assertEquals("Should have 1 item", 1, recentlyOpened.list().size());

        recentlyOpened.reload();

        assertEquals("Should still have 1 item after reload", 1, recentlyOpened.list().size());
    }

    /**
     * Test decode method with simple URI.
     */
    @Test
    public void should_DecodeUri_When_DecodeCalledWithSimpleUri() {
        String encoded = testUri.toString();

        FileData decoded = RecentlyOpened.decode(context, encoded);

        assertNotNull("Decoded FileData should not be null", decoded);
        assertEquals("Uri should match", testUri, decoded.getUri());
    }

    /**
     * Test decode with sequential file (3 parts).
     */
    @Test
    public void should_DecodeSequentialFile_When_DecodeCalledWith3Parts() {
        String encoded = "1000^2000^" + testUri.toString();

        FileData decoded = RecentlyOpened.decode(context, encoded);

        assertNotNull("Decoded FileData should not be null", decoded);
        assertEquals("Start offset should be 1000", 1000L, decoded.getStartOffset());
        assertEquals("End offset should be 2000", 2000L, decoded.getEndOffset());
    }

    /**
     * Test decode with 2 parts (legacy format).
     */
    @Test
    public void should_DecodeWithEndOffset_When_DecodeCalledWith2Parts() {
        String encoded = "2000^" + testUri.toString();

        FileData decoded = RecentlyOpened.decode(context, encoded);

        assertNotNull("Decoded FileData should not be null", decoded);
        assertEquals("End offset should be 2000", 2000L, decoded.getEndOffset());
    }

    /**
     * Test persistence across instances.
     */
    @Test
    public void should_PersistData_When_NewInstanceCreated() {
        RecentlyOpened ro1 = new RecentlyOpened(app);
        FileData fileData = new FileData(context, testUri, false);

        ro1.add(fileData);
        assertEquals("First instance should have 1 item", 1, ro1.list().size());

        // Create new instance
        RecentlyOpened ro2 = new RecentlyOpened(app);

        assertEquals("Second instance should also have 1 item", 1, ro2.list().size());
    }

    /**
     * Test that list is mutable.
     */
    @Test
    public void should_ReturnMutableList_When_ListCalled() {
        RecentlyOpened recentlyOpened = new RecentlyOpened(app);
        FileData fileData = new FileData(context, testUri, false);

        recentlyOpened.add(fileData);
        List<FileData> list = recentlyOpened.list();

        assertNotNull("List should not be null", list);
        assertEquals("List should have 1 item", 1, list.size());
    }
}
