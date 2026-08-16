/**
 *******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Task used to open a file.
 * </p>
 * @author Keidan
 *
 *******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.task;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.text.format.Formatter;
import android.util.Log;

import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.adt.AdtHexTextArray;
import com.galaxyjoy.hexviewer.ui.util.UIHelper;
import com.galaxyjoy.hexviewer.util.SysHelper;
import com.galaxyjoy.hexviewer.util.io.RandomAccessFileChannel;
import com.galaxyjoy.hexviewer.util.memory.MemoryInfo;
import com.galaxyjoy.hexviewer.util.memory.MemoryListener;
import com.galaxyjoy.hexviewer.util.memory.MemoryMonitor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskOpen extends ProgressTask<ContentResolver, FileData, TaskOpen.Result> implements MemoryListener {
    private final Context mContext;
    // Use centralized constant: SysHelper.MAX_BY_ROW_16 * AppConstants.FILE_BUFFER_ROWS
    private static final int MAX_LENGTH = com.galaxyjoy.hexviewer.constants.AppConstants.MAX_FILE_BUFFER_SIZE;
    private final AdtHexTextArray mAdapter;
    private final OpenResultListener mListener;
    private RandomAccessFileChannel mRandomAccessFileChannel = null;
    private final boolean mAddRecent;
    private final ContentResolver mContentResolver;
    private final MemoryMonitor mMemoryMonitor;
    private final AtomicBoolean mLowMemory = new AtomicBoolean(false);
    private final String mOldToString;
    private final MyApplication mApp;

    public static class Result {
        private List<LineEntry> listHex = null;
        private String exception = null;
        private long startOffset = 0;
    }

    public interface OpenResultListener {
        void onOpenResult(boolean success, boolean fromOpen);
    }

    public TaskOpen(final Activity activity,
                    final AdtHexTextArray adapter,
                    final OpenResultListener listener,
                    final String oldToString,
                    final boolean addRecent) {
        super(activity, true);
        mApp = (MyApplication) activity.getApplicationContext();
        // Kiểm tra memory mỗi 500ms thay vì 2000ms để phát hiện low memory nhanh hơn
        // khi đang load file lớn trên thiết bị MediaTek RAM thấp
        mMemoryMonitor = new MemoryMonitor(mApp.getMemoryThreshold(), 500);
        mContext = activity;
        mContentResolver = activity.getContentResolver();
        mAdapter = adapter;
        mListener = listener;
        mAddRecent = addRecent;
        mOldToString = oldToString;
    }

    /**
     * Called before the execution of the task.
     *
     * @return The Config.
     */
    @Override
    public ContentResolver onPreExecute() {
        super.onPreExecute();
        mLowMemory.set(false);
        mMemoryMonitor.start(this, true);
        mAdapter.clear();
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
        mMemoryMonitor.stop();
        Log.d("roy93~", "TaskOpen.onPostExecute: mLowMemory=" + mLowMemory.get() 
            + ", isCancelled=" + isCancelled() 
            + ", exception=" + (result == null ? "null" : result.exception));
        if (mLowMemory.get())
            UIHelper.showErrorDialog(mContext, R.string.error_title, mContext.getString(R.string.not_enough_memory));
        else if (isCancelled())
            UIHelper.toast(mContext, mContext.getString(R.string.operation_canceled));
        else if (result.exception != null) {
            String lowercaseMessage = result.exception.toLowerCase(Locale.US);
            if (lowercaseMessage.contains("com.android.externalstorage") && 
                (lowercaseMessage.contains("has no access") || lowercaseMessage.contains("permission denial"))) {
                UIHelper.showErrorDialog(mContext, R.string.error_system_storage_bug_title, mContext.getString(R.string.error_system_storage_bug_msg));
            } else {
                UIHelper.showErrorDialog(mContext, R.string.error_title, mContext.getString(R.string.exception) + ": " + result.exception);
            }
        } else {
            if (result.listHex != null) {
                try {
                    mAdapter.setStartOffset(result.startOffset);
                    Log.d("roy93~", "TaskOpen.onPostExecute: calling mAdapter.addAll with " + result.listHex.size() + " items");
                    mAdapter.addAll(result.listHex);
                    Log.d("roy93~", "TaskOpen.onPostExecute: mAdapter.addAll completed successfully");
                } catch (OutOfMemoryError oom) {
                    Log.e("roy93~", "TaskOpen.onPostExecute: mAdapter.addAll caught OutOfMemoryError!", oom);
                    // Handle OOM when adding to adapter
                    result.listHex.clear(); // Release memory
                    result.listHex = null;
                    System.gc();
                    // Set exception so it's handled by the normal error flow
                    result.exception = "OutOfMemoryError: " + oom.getMessage();
                    UIHelper.showErrorDialog(mContext, R.string.error_title, mContext.getString(R.string.not_enough_memory));
                }
            }
        }
        if (!mLowMemory.get()) {
            MemoryInfo mi = mMemoryMonitor.getLastMemoryInfo();
            MyApplication.addLog(mContext, "Open",
                    String.format(Locale.US, "Memory status, used: %s (%.02f%%), free: %s, max: %s",
                            Formatter.formatFileSize(mContext, mi.getUsedMemory()), mi.getPercentUsed(),
                            Formatter.formatFileSize(mContext, mi.getTotalFreeMemory()),
                            Formatter.formatFileSize(mContext, mi.getTotalMemory())));
        }
        if (mListener != null)
            mListener.onOpenResult(result.exception == null && !isCancelled() && !mLowMemory.get(), true);
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
        close();
        if (mListener != null)
            mListener.onOpenResult(false, true);
    }

    private void processRead(final FileData fd,
                             final List<LineEntry> list,
                             final Result result,
                             long totalSequential,
                             int maxLength) throws IOException {
        boolean first = true;
        int reads;
        boolean forceBreak = false;
        /* read data */
        ByteBuffer buffer = ByteBuffer.allocate(maxLength);
        int loopCount = 0;
        while (!isCancelled()) {
            buffer.clear();
            if (fd.isSequential()) {
                long remaining = fd.getEndOffset() - totalSequential;
                if (remaining <= 0L) break;
                buffer.limit((int) Math.min(buffer.capacity(), remaining));
            }
            reads = mRandomAccessFileChannel.read(buffer);
            if (reads == -1) break;
            loopCount++;
            if (loopCount % 50 == 0 || loopCount < 5) {
                Runtime rt = Runtime.getRuntime();
                long usedHeap = rt.totalMemory() - rt.freeMemory();
                long freeHeap = rt.maxMemory() - usedHeap;
                Log.d("roy93~", "TaskOpen.processRead loop=" + loopCount 
                    + ", reads=" + reads 
                    + ", freeHeap=" + (freeHeap / 1024 / 1024) + "MB"
                    + ", maxHeap=" + (rt.maxMemory() / 1024 / 1024) + "MB");
            }
            try {
                SysHelper.formatBuffer(list,
                        buffer.array(),
                        reads,
                        mCancel,
                        mApp.getNbBytesPerLine(),
                        first ? fd.getShiftOffset() : 0);
                first = false;
                publishProgress((long) reads);
                if (fd.isSequential()) {
                    totalSequential += reads;
                    if (totalSequential >= fd.getEndOffset()) {
                        Log.d("roy93~", "TaskOpen.processRead: sequential offset reached: " + totalSequential);
                        forceBreak = true;
                    }
                }
                // ★ OOM Guard: kiểm tra MemoryMonitor báo low memory
                if (mLowMemory.get()) {
                    Log.d("roy93~", "TaskOpen.processRead: mLowMemory is true, breaking");
                    forceBreak = true;
                } else {
                    // ★ Proactive heap check: dừng ngay nếu heap còn < 20 MB
                    // Ngăn MediaTek BoostFwk OOM trước khi MemoryMonitor kịp phản ứng
                    Runtime rt = Runtime.getRuntime();
                    long usedHeap = rt.totalMemory() - rt.freeMemory();
                    long freeHeap = rt.maxMemory() - usedHeap;
                    if (freeHeap < 20L * 1024 * 1024) { // còn < 20 MB
                        Log.d("roy93~", "TaskOpen.processRead: freeHeap < 20MB! " + (freeHeap / 1024 / 1024) + "MB, aborting");
                        mLowMemory.set(true);
                        mCancel.set(true);
                        forceBreak = true;
                    }
                }
            } catch (OutOfMemoryError oom) {
                Log.e("roy93~", "TaskOpen.processRead: Caught OutOfMemoryError!", oom);
                list.clear();
                System.gc();
                mLowMemory.set(true);
                mCancel.set(true);
                forceBreak = true;
            } catch (IllegalArgumentException iae) {
                Log.e("roy93~", "TaskOpen.processRead: IllegalArgumentException: " + iae.getMessage());
                result.exception = iae.getMessage();
                forceBreak = true;
            }
            if (forceBreak)
                break;
        }
    }

    /**
     * Performs a computation on a background thread.
     *
     * @param contentResolver ContentResolver.
     * @param fd              FileData.
     * @return The result.
     */
    @Override
    public Result doInBackground(ContentResolver contentResolver, FileData fd) {
        Log.d("roy93~", "TaskOpen.doInBackground: Uri=" + fd.getUri()
            + ", size=" + fd.getSize()
            + ", realSize=" + fd.getRealSize()
            + ", isSequential=" + fd.isSequential()
            + ", startOffset=" + fd.getStartOffset()
            + ", endOffset=" + fd.getEndOffset());
        final Result result = new Result();
        final List<LineEntry> list = new ArrayList<>();
        try {
            result.startOffset = fd.getStartOffset();
            /* Size + stream */
            mTotalSize = fd.getSize();

            publishProgress(0L);

            if (fd.isStreaming()) {
                try (com.galaxyjoy.hexviewer.streaming.StreamingSession session =
                             new com.galaxyjoy.hexviewer.streaming.StreamingSession(
                                     com.galaxyjoy.hexviewer.streaming.SeekableDataSourceFactory.openContentUri(
                                             contentResolver, fd.getUri(), mContext.getCacheDir(),
                                             fd.getRealSize(),
                                             (copied, expected) -> publishProgress(copied),
                                             this::isCancelled))) {
                    if (fd.isSizeUnknown()) {
                        fd.setResolvedRealSize(session.size());
                        long end = Math.min(fd.getRealSize(),
                                com.galaxyjoy.hexviewer.constants.AppConstants.STREAMING_WINDOW_SIZE);
                        fd.setStreamingWindow(0L, end);
                        mTotalSize = fd.getSize();
                    }
                    result.startOffset = fd.getStartOffset();
                    if (!validateFileSize(fd, result)) return result;
                    com.galaxyjoy.hexviewer.streaming.WindowRange range =
                            new com.galaxyjoy.hexviewer.streaming.WindowRange(
                                    fd.getStartOffset(), fd.getEndOffset());
                    byte[] resident = session.read(range);
                    evaluateShiftOffset(fd, fd.getStartOffset());
                    SysHelper.formatBuffer(list, resident, resident.length, mCancel,
                            mApp.getNbBytesPerLine(), fd.getShiftOffset());
                    publishProgress((long) resident.length);
                    if (!mCancel.get()) {
                        result.listHex = list;
                        if (mOldToString != null)
                            mApp.getRecentlyOpened().remove(mOldToString);
                        if (mAddRecent)
                            mApp.getRecentlyOpened().add(fd);
                    }
                }
                return result;
            }

            // Validate legacy full/partial opens before allocating their buffers.
            if (!validateFileSize(fd, result)) {
                Log.d("roy93~", "TaskOpen.doInBackground: validateFileSize failed!");
                return result; // result.exception already set
            }

            mRandomAccessFileChannel = RandomAccessFileChannel.openForReadOnly(contentResolver, fd.getUri());

            int maxLength = moveCursorIfSequential(fd, result);

            if (result.exception == null) {
                /* prepare buffer */
                long totalSequential = fd.getStartOffset();
                evaluateShiftOffset(fd, totalSequential);
                processRead(fd, list, result, totalSequential, maxLength);
                /* prepare result */
                if (result.exception == null && !mLowMemory.get()) {
                    result.listHex = list;
                    if (!mCancel.get()) {
                        if (mOldToString != null)
                            mApp.getRecentlyOpened().remove(mOldToString);
                        if (mAddRecent)
                            mApp.getRecentlyOpened().add(fd);
                    }
                }
            }
        } catch (OutOfMemoryError oom) {
            // ★ Last-resort OOM catch: giải phóng list và báo lỗi
            list.clear();
            result.listHex = null;
            System.gc();
            mLowMemory.set(true);
        } catch (final Exception e) {
            result.exception = e.getMessage();
        } finally {
            close();
        }
        return result;
    }

    /**
     * Validates file size to prevent OOM errors.
     *
     * @param fd FileData to validate
     * @param result Result object to set exception message if validation fails
     * @return true if file size is valid, false otherwise
     */
    private boolean validateFileSize(FileData fd, Result result) {
        long fileSize = fd.getSize();

        // Legacy full/partial modes retain their historical bound. Streaming uses
        // long offsets and is limited by the provider instead of an arbitrary 2 GiB cap.
        if (!fd.isStreaming() && fileSize > com.galaxyjoy.hexviewer.constants.AppConstants.ABSOLUTE_MAX_FILE_SIZE) {
            String maxSizeStr = SysHelper.sizeToHuman(mContext,
                com.galaxyjoy.hexviewer.constants.AppConstants.ABSOLUTE_MAX_FILE_SIZE,
                true, true, false);
            result.exception = "File too large: " + SysHelper.sizeToHuman(mContext, fileSize, true, true, false) +
                ". Maximum supported: " + maxSizeStr;
            return false;
        }

        // Check normal mode limit - if file is larger than 30MB, ask user to use sequential (partial) mode
        if (!fd.isStreaming() && !fd.isSequential() && fileSize > com.galaxyjoy.hexviewer.constants.AppConstants.MAX_NORMAL_FILE_SIZE) {
            String maxSizeStr = SysHelper.sizeToHuman(mContext,
                com.galaxyjoy.hexviewer.constants.AppConstants.MAX_NORMAL_FILE_SIZE,
                true, true, false);
            Log.d("roy93~", "TaskOpen.validateFileSize: File too large for normal mode! size=" + fileSize + " > " + com.galaxyjoy.hexviewer.constants.AppConstants.MAX_NORMAL_FILE_SIZE);
            result.exception = "File too large to open entirely: " + SysHelper.sizeToHuman(mContext, fileSize, true, true, false) +
                ". Maximum for full open is: " + maxSizeStr + ". Please use 'Sequential opening' mode instead.";
            return false;
        }

        // Check sequential mode portion size limit (must be <= 30MB)
        if (!fd.isStreaming() && fd.isSequential() && fileSize > com.galaxyjoy.hexviewer.constants.AppConstants.MAX_NORMAL_FILE_SIZE) {
            String maxSizeStr = SysHelper.sizeToHuman(mContext,
                com.galaxyjoy.hexviewer.constants.AppConstants.MAX_NORMAL_FILE_SIZE,
                true, true, false);
            Log.d("roy93~", "TaskOpen.validateFileSize: Portion too large for sequential mode! portionSize=" + fileSize + " > " + com.galaxyjoy.hexviewer.constants.AppConstants.MAX_NORMAL_FILE_SIZE);
            result.exception = "Selected portion is too large: " + SysHelper.sizeToHuman(mContext, fileSize, true, true, false) +
                ". Maximum portion size is: " + maxSizeStr;
            return false;
        }

        // Check sequential mode absolute file limit (real file size must be <= 2GB)
        if (!fd.isStreaming() && fd.isSequential() && fd.getRealSize() > com.galaxyjoy.hexviewer.constants.AppConstants.MAX_SEQUENTIAL_FILE_SIZE) {
            String maxSizeStr = SysHelper.sizeToHuman(mContext,
                com.galaxyjoy.hexviewer.constants.AppConstants.MAX_SEQUENTIAL_FILE_SIZE,
                true, true, false);
            Log.d("roy93~", "TaskOpen.validateFileSize: File too large for sequential mode! realSize=" + fd.getRealSize() + " > " + com.galaxyjoy.hexviewer.constants.AppConstants.MAX_SEQUENTIAL_FILE_SIZE);
            result.exception = "File too large for sequential mode: " + SysHelper.sizeToHuman(mContext, fd.getRealSize(), true, true, false) +
                ". Maximum file size for sequential mode is: " + maxSizeStr;
            return false;
        }

        Log.d("roy93~", "TaskOpen.validateFileSize: Validation PASSED! size=" + fileSize + ", isSequential=" + fd.isSequential());

        // Warn about large files but allow opening
        if (fileSize > com.galaxyjoy.hexviewer.constants.AppConstants.RECOMMENDED_MAX_FILE_SIZE) {
            MyApplication.addLog(mContext, "TaskOpen",
                "Warning: Opening large file (" + SysHelper.sizeToHuman(mContext, fileSize, true, true, false) +
                "). This may take time and consume significant memory.");
        }

        return true;
    }

    private int moveCursorIfSequential(FileData fd, Result result) {
        int maxLength = MAX_LENGTH;
        if (fd.isSequential()) {
            mRandomAccessFileChannel.setPosition(fd.getStartOffset());
            if (mRandomAccessFileChannel.getPosition() != fd.getStartOffset()) {
                result.exception = "Unable to skip file data!";
            }
            maxLength = fd.getSize() < MAX_LENGTH ? (int) fd.getSize() : MAX_LENGTH;
        }
        return maxLength;
    }

    private void evaluateShiftOffset(FileData fd, long totalSequential) {
        if (totalSequential != 0) {
            final int nbBytesPerLine = mApp.getNbBytesPerLine();
            final long count = totalSequential / nbBytesPerLine;
            final long remain = totalSequential - (count * nbBytesPerLine);
            fd.setShiftOffset((int) remain);
        }
    }

    public void onLowAppMemory(boolean disabled, MemoryInfo mi) {
        Log.d("roy93~", "TaskOpen.onLowAppMemory: disabled=" + disabled 
            + ", totalFreeMemory=" + mi.getTotalFreeMemory() 
            + ", totalMemory=" + mi.getTotalMemory());
        MyApplication.addLog(mContext, "Open",
                String.format(Locale.US, "Low memory %s, used: %s (%.02f%%), free: %s, max: %s",
                        disabled ? "disabled" : "detected",
                        Formatter.formatFileSize(mContext, mi.getUsedMemory()), mi.getPercentUsed(),
                        Formatter.formatFileSize(mContext, mi.getTotalFreeMemory()),
                        Formatter.formatFileSize(mContext, mi.getTotalMemory())));
        if (!disabled) {
            mLowMemory.set(true);
            mCancel.set(true);
        }
    }
}
