package com.galaxyjoy.hexviewer.ui.task;

import android.content.ContentResolver;
import android.net.Uri;

import com.galaxyjoy.hexviewer.ui.adt.AdtHashResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class TaskHashTest {

    @Mock
    private ContentResolver mockContentResolver;

    @Mock
    private Uri mockUri;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ========== TaskHash.Result unit tests ==========

    @Test
    public void should_ReturnTrue_When_ResultIsSuccess() {
        List<AdtHashResult.HashItem> items = new ArrayList<>();
        TaskHash.Result result = new TaskHash.Result(items, null, false);
        assertTrue(result.isSuccess());
        assertFalse(result.isCancelled());
        assertNull(result.errorMessage);
        assertEquals(items, result.items);
    }

    @Test
    public void should_ReturnFalse_When_ResultHasError() {
        TaskHash.Result result = new TaskHash.Result(null, "Error occurred", false);
        assertFalse(result.isSuccess());
        assertFalse(result.isCancelled());
        assertEquals("Error occurred", result.errorMessage);
        assertNull(result.items);
    }

    @Test
    public void should_ReturnFalse_When_ResultIsCancelled() {
        TaskHash.Result result = new TaskHash.Result(null, null, true);
        assertFalse(result.isSuccess());
        assertTrue(result.isCancelled());
        assertNull(result.errorMessage);
        assertNull(result.items);
    }

    @Test
    public void should_ReturnFalseSuccess_When_ResultHasErrorAndCancelled() {
        TaskHash.Result result = new TaskHash.Result(null, "Error but cancelled", true);
        assertFalse(result.isSuccess());
        assertTrue(result.isCancelled());
    }

    // ========== TaskHash doInBackground & async tests ==========

    @Test
    public void should_ComputeCorrectHashes_When_ValidStreamProvided() throws Exception {
        String testInput = "Hello, World!";
        byte[] inputBytes = testInput.getBytes("UTF-8");
        InputStream inputStream = new ByteArrayInputStream(inputBytes);

        when(mockContentResolver.openInputStream(mockUri)).thenReturn(inputStream);

        TaskHash task = new TaskHash(null);
        TaskHash.Request request = new TaskHash.Request(mockContentResolver, mockUri);
        TaskHash.Result result = task.doInBackground(null, request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.items);
        assertEquals(4, result.items.size());

        // Expected hashes for "Hello, World!":
        // MD5: 65a8e27d8879283831b664bd8b7f0ad4
        // SHA-1: 0a0a9f2a6772942557ab5355d76af442f8f65e01
        // SHA-256: dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f
        // SHA-512: 374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387

        assertEquals("MD5", result.items.get(0).algorithm);
        assertEquals("65a8e27d8879283831b664bd8b7f0ad4", result.items.get(0).value);

        assertEquals("SHA-1", result.items.get(1).algorithm);
        assertEquals("0a0a9f2a6772942557ab5355d76af442f8f65e01", result.items.get(1).value);

        assertEquals("SHA-256", result.items.get(2).algorithm);
        assertEquals("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f", result.items.get(2).value);

        assertEquals("SHA-512", result.items.get(3).algorithm);
        assertEquals("374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387", result.items.get(3).value);
    }

    @Test
    public void should_ReturnErrorResult_When_StreamIsNull() throws Exception {
        when(mockContentResolver.openInputStream(mockUri)).thenReturn(null);

        TaskHash task = new TaskHash(null);
        TaskHash.Request request = new TaskHash.Request(mockContentResolver, mockUri);
        TaskHash.Result result = task.doInBackground(null, request);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertContains(result.errorMessage, "Cannot open stream");
    }

    @Test
    public void should_ReturnErrorResult_When_ExceptionThrown() throws Exception {
        when(mockContentResolver.openInputStream(mockUri)).thenThrow(new RuntimeException("Simulated IO failure"));

        TaskHash task = new TaskHash(null);
        TaskHash.Request request = new TaskHash.Request(mockContentResolver, mockUri);
        TaskHash.Result result = task.doInBackground(null, request);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Simulated IO failure", result.errorMessage);
    }

    @Test
    public void should_CallListenerWithCancelledResult_When_onCancelledInvoked() {
        final AtomicReference<TaskHash.Result> deliveredResult = new AtomicReference<>();
        TaskHash task = new TaskHash(new TaskHash.HashResultListener() {
            @Override
            public void onHashResult(TaskHash.Result result) {
                deliveredResult.set(result);
            }
        });

        task.onCancelled();

        assertNotNull(deliveredResult.get());
        assertTrue(deliveredResult.get().isCancelled());
        assertFalse(deliveredResult.get().isSuccess());
    }

    @Test
    public void should_CallListenerWithErrorResult_When_onExceptionInvoked() {
        final AtomicReference<TaskHash.Result> deliveredResult = new AtomicReference<>();
        TaskHash task = new TaskHash(new TaskHash.HashResultListener() {
            @Override
            public void onHashResult(TaskHash.Result result) {
                deliveredResult.set(result);
            }
        });

        task.onException(new RuntimeException("Oops"));

        assertNotNull(deliveredResult.get());
        assertFalse(deliveredResult.get().isSuccess());
        assertFalse(deliveredResult.get().isCancelled());
        assertEquals("Oops", deliveredResult.get().errorMessage);
    }

    @Test
    public void should_CallListenerWithPostExecuteResult_When_onPostExecuteInvoked() {
        final AtomicReference<TaskHash.Result> deliveredResult = new AtomicReference<>();
        TaskHash task = new TaskHash(new TaskHash.HashResultListener() {
            @Override
            public void onHashResult(TaskHash.Result result) {
                deliveredResult.set(result);
            }
        });

        List<AdtHashResult.HashItem> items = new ArrayList<>();
        TaskHash.Result result = new TaskHash.Result(items, null, false);
        task.onPostExecute(result);

        assertNotNull(deliveredResult.get());
        assertTrue(deliveredResult.get().isSuccess());
        assertEquals(items, deliveredResult.get().items);
    }

    private void assertContains(String outer, String inner) {
        assertNotNull("String should not be null", outer);
        assertTrue("Expected '" + outer + "' to contain '" + inner + "'", outer.contains(inner));
    }
}
