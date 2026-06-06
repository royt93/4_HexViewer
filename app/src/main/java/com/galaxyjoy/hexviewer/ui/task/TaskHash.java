/**
 *******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Background task for computing file hash values (MD5, SHA-1, SHA-256, SHA-512)
 * in a single streaming pass.
 * </p>
 *
 * @author mckimquyen
 * <p>
 * License: GPLv3
 * </p>
 *******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.task;

import android.content.ContentResolver;
import android.net.Uri;

import com.galaxyjoy.hexviewer.ui.adt.AdtHashResult;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes MD5, SHA-1, SHA-256 and SHA-512 hashes for a given {@link Uri} in a
 * single streaming pass to minimise I/O overhead.
 *
 * <p>Follows the {@link TaskRunner} pattern used throughout the codebase so that
 * cancellation, progress and memory-leak prevention are handled uniformly.</p>
 */
public class TaskHash extends TaskRunner<Void, TaskHash.Request, Void, TaskHash.Result> {

    // -------------------------------------------------------------------------
    // Request / Result value objects
    // -------------------------------------------------------------------------

    public static final class Request {
        public final ContentResolver contentResolver;
        public final Uri uri;

        public Request(ContentResolver contentResolver, Uri uri) {
            this.contentResolver = contentResolver;
            this.uri = uri;
        }
    }

    public static final class Result {
        /** Non-null on success; contains MD5, SHA-1, SHA-256, SHA-512 items in that order. */
        public final List<AdtHashResult.HashItem> items;
        /** Non-null when an error occurred. */
        public final String errorMessage;
        /** True when the task was cancelled by the caller. */
        public final boolean cancelled;

        Result(List<AdtHashResult.HashItem> items, String errorMessage, boolean cancelled) {
            this.items = items;
            this.errorMessage = errorMessage;
            this.cancelled = cancelled;
        }

        public boolean isSuccess() {
            return errorMessage == null && !cancelled;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    // -------------------------------------------------------------------------
    // Listener
    // -------------------------------------------------------------------------

    public interface HashResultListener {
        void onHashResult(Result result);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final HashResultListener mListener;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public TaskHash(HashResultListener listener) {
        mListener = listener;
    }

    // -------------------------------------------------------------------------
    // TaskRunner overrides
    // -------------------------------------------------------------------------

    @Override
    public Result doInBackground(Void config, Request request) {
        try (InputStream inputStream = request.contentResolver.openInputStream(request.uri)) {
            if (inputStream == null) {
                return new Result(null, "Cannot open stream for URI: " + request.uri, false);
            }

            MessageDigest md5    = MessageDigest.getInstance("MD5");
            MessageDigest sha1   = MessageDigest.getInstance("SHA-1");
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");

            byte[] buffer = new byte[8192];
            int bytesRead;
            while (!isCancelled() && (bytesRead = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, bytesRead);
                sha1.update(buffer, 0, bytesRead);
                sha256.update(buffer, 0, bytesRead);
                sha512.update(buffer, 0, bytesRead);
            }

            if (isCancelled()) {
                return new Result(null, null, true);
            }

            List<AdtHashResult.HashItem> items = new ArrayList<>(4);
            items.add(new AdtHashResult.HashItem("MD5",     bytesToHex(md5.digest())));
            items.add(new AdtHashResult.HashItem("SHA-1",   bytesToHex(sha1.digest())));
            items.add(new AdtHashResult.HashItem("SHA-256", bytesToHex(sha256.digest())));
            items.add(new AdtHashResult.HashItem("SHA-512", bytesToHex(sha512.digest())));
            return new Result(items, null, false);

        } catch (Exception e) {
            return new Result(null, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(), false);
        }
    }

    @Override
    public void onPostExecute(Result result) {
        if (mListener != null) {
            mListener.onHashResult(result);
        }
    }

    /**
     * Called when the task is cancelled via {@link #cancel()}.
     * Notifies the listener so the caller can hide progress indicators.
     */
    @Override
    public void onCancelled() {
        if (mListener != null) {
            mListener.onHashResult(new Result(null, null, true));
        }
    }

    /**
     * Called when an unexpected exception escapes {@link #doInBackground}.
     * Forwards the error to the listener so the caller can show an error message.
     *
     * @param t The exception that was thrown.
     */
    @Override
    public void onException(Throwable t) {
        if (mListener != null) {
            String msg = (t.getMessage() != null) ? t.getMessage() : t.getClass().getSimpleName();
            mListener.onHashResult(new Result(null, msg, false));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
