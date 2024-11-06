package com.galaxyjoy.hexviewer.ui.tasks;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.text.format.Formatter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import com.galaxyjoy.hexviewer.ui.adapters.HexTextArrayAdapter;
import com.galaxyjoy.hexviewer.ui.utils.UIHelper;
import com.galaxyjoy.hexviewer.ApplicationCtx;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.utils.SysHelper;
import com.galaxyjoy.hexviewer.utils.io.RandomAccessFileChannel;
import com.galaxyjoy.hexviewer.utils.memory.MemoryInfo;
import com.galaxyjoy.hexviewer.utils.memory.MemoryListener;
import com.galaxyjoy.hexviewer.utils.memory.MemoryMonitor;

/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Task used to open a file.
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
public class TaskOpen extends ProgressTask<ContentResolver, FileData, TaskOpen.Result> implements MemoryListener {
  private final Context mContext;
  private static final int MAX_LENGTH = SysHelper.MAX_BY_ROW_16 * 20000;
  private final HexTextArrayAdapter mAdapter;
  private final OpenResultListener mListener;
  private RandomAccessFileChannel mRandomAccessFileChannel = null;
  private final boolean mAddRecent;
  private final ContentResolver mContentResolver;
  private final MemoryMonitor mMemoryMonitor;
  private final AtomicBoolean mLowMemory = new AtomicBoolean(false);
  private final String mOldToString;
  private final ApplicationCtx mApp;

  public static class Result {
    private List<LineEntry> listHex = null;
    private String exception = null;
    private long startOffset = 0;
  }

  public interface OpenResultListener {
    void onOpenResult(boolean success, boolean fromOpen);
  }

  public TaskOpen(final Activity activity,
                  final HexTextArrayAdapter adapter,
                  final OpenResultListener listener, final String oldToString, final boolean addRecent) {
    super(activity, true);
    mApp = (ApplicationCtx) activity.getApplicationContext();
    mMemoryMonitor = new MemoryMonitor(mApp.getMemoryThreshold(), 2000);
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
    if (mLowMemory.get())
      UIHelper.showErrorDialog(mContext, R.string.error_title, mContext.getString(R.string.not_enough_memory));
    else if (isCancelled())
      UIHelper.toast(mContext, mContext.getString(R.string.operation_canceled));
    else if (result.exception != null)
      UIHelper.showErrorDialog(mContext, R.string.error_title, mContext.getString(R.string.exception) + ": " + result.exception);
    else {
      if (result.listHex != null) {
        mAdapter.setStartOffset(result.startOffset);
        mAdapter.addAll(result.listHex);
      }
    }
    if (!mLowMemory.get()) {
      MemoryInfo mi = mMemoryMonitor.getLastMemoryInfo();
      ApplicationCtx.addLog(mContext, "Open",
        String.format(Locale.US, "Memory status, used: %s (%.02f%%), free: %s, max: %s",
          Formatter.formatFileSize(mContext, mi.getUsedMemory()), mi.getPercentUsed(),
          Formatter.formatFileSize(mContext, mi.getTotalFreeMemory()),
          Formatter.formatFileSize(mContext, mi.getTotalMemory())));
    }
    if (mListener != null)
      mListener.onOpenResult(result.exception == null && !isCancelled() && !mLowMemory.get(), true);
    super.onPostExecute(result);
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
    while (!isCancelled() && (reads = mRandomAccessFileChannel.read(buffer)) != -1) {
      try {
        SysHelper.formatBuffer(list, buffer.array(), reads, mCancel,
          mApp.getNbBytesPerLine(), first ? fd.getShiftOffset() : 0);
        first = false;
        buffer.clear();
        publishProgress((long) reads);
        if (fd.isSequential()) {
          totalSequential += reads;
          if (totalSequential >= fd.getEndOffset())
            forceBreak = true;
        }
      } catch (IllegalArgumentException iae) {
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
    final Result result = new Result();
    final List<LineEntry> list = new ArrayList<>();
    try {
      result.startOffset = fd.getStartOffset();
      /* Size + stream */
      mTotalSize = fd.getSize();
      publishProgress(0L);
      mRandomAccessFileChannel = RandomAccessFileChannel.openForReadOnly(contentResolver, fd.getUri());

      int maxLength = moveCursorIfSequential(fd, result);

      if (result.exception == null) {
        /* prepare buffer */
        long totalSequential = fd.getStartOffset();
        evaluateShiftOffset(fd, totalSequential);
        processRead(fd, list, result, totalSequential, maxLength);
        /* prepare result */
        if (result.exception == null) {
          result.listHex = list;
          if (!mCancel.get()) {
            if (mOldToString != null)
              mApp.getRecentlyOpened().remove(mOldToString);
            if (mAddRecent)
              mApp.getRecentlyOpened().add(fd);
          }
        }
      }
    } catch (final Exception e) {
      result.exception = e.getMessage();
    } finally {
      close();
    }
    return result;
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
    ApplicationCtx.addLog(mContext, "Open",
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
