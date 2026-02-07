/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Generic task with progress.
 * </p>
 *
 * @author Keidan
 * <p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.task;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.util.SysHelper;

import java.lang.ref.WeakReference;

public abstract class ProgressTask<C, P, T> extends TaskRunner<C, P, Long, T> {
    private final AlertDialog mDialog;
    protected final TextView mTextView;
    protected long mTotalSize = 0L;
    protected long mCurrentSize = 0L;
    private final String progressText;
    private final WeakReference<Activity> mActivityRef;

    ProgressTask(final Activity activity, boolean loading) {
        mActivityRef = new WeakReference<>(activity);
        progressText = activity.getString(loading ? R.string.loading : R.string.saving) + " ";
        mDialog = new AlertDialog.Builder(activity).create();
        mDialog.setCancelable(false);
        if (mDialog.getWindow() != null) {
            mDialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_alert_dialog);
        }
        final View v = activity.getLayoutInflater().inflate(R.layout.v_progress_dialog, null);
        mTextView = v.findViewById(R.id.text);
        mTextView.setText(loading ? R.string.loading : R.string.saving);
        v.findViewById(R.id.cancel).setOnClickListener(view -> {
            cancel();
            dismissDialogSafely();
        });
        mDialog.setView(v);
    }

    /**
     * Checks if the dialog can be safely dismissed.
     * This prevents IllegalArgumentException when Activity is destroyed.
     *
     * @return true if dialog can be dismissed safely.
     */
    private boolean isDialogSafe() {
        if (mDialog == null)
            return false;
        Activity activity = mActivityRef.get();
        if (activity == null)
            return false;
        if (activity.isFinishing())
            return false;
        if (activity.isDestroyed())
            return false;
        return mDialog.isShowing();
    }

    /**
     * Safely dismisses the dialog, checking Activity state first.
     */
    private void dismissDialogSafely() {
        try {
            if (isDialogSafe()) {
                mDialog.dismiss();
            }
        } catch (IllegalArgumentException e) {
            // View not attached to window manager, ignore
        }
    }

    /**
     * Runs on the UI thread.
     *
     * @param value The value indicating progress.
     */
    @Override
    public void onProgressUpdate(Long value) {
        mCurrentSize += value;
        String text = progressText;
        text += "\n";
        text += SysHelper.sizeToHuman(mTextView.getContext(), mCurrentSize) + " / "
                + SysHelper.sizeToHuman(mTextView.getContext(), mTotalSize);
        mTextView.setText(text);
    }

    /**
     * Called before the execution of the task.
     *
     * @return The Config.
     */
    @Override
    public C onPreExecute() {
        mCurrentSize = 0L;
        if (mDialog != null)
            mDialog.show();
        return null;
    }

    /**
     * Called after the execution of the task.
     *
     * @param result The result.
     */
    @Override
    public void onPostExecute(final T result) {
        dismissDialogSafely();
    }

    /**
     * Called when the async task is cancelled.
     */
    @Override
    public void onCancelled() {
        dismissDialogSafely();
    }

}
