/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Undo Redo Manager
 * </p>
 *
 * @author Keidan
 * <p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.undoredo;

import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.act.ActMain;
import com.galaxyjoy.hexviewer.ui.undoredo.commands.DeleteCommand;
import com.galaxyjoy.hexviewer.ui.undoredo.commands.UpdateAndDeleteCommand;
import com.galaxyjoy.hexviewer.ui.undoredo.commands.UpdateCommand;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public class UnDoRedo {
    private static final int CONTROL_UNDO = 0;
    private static final int CONTROL_REDO = 1;
    // Use WeakReference to prevent Activity leaks if UnDoRedo outlives Activity
    private final WeakReference<ActMain> mActivityRef;
    private final Control[] mControls;
    private final Deque<ICommand> mUndo;
    private final Deque<ICommand> mRedo;
    private int mReferenceIndex;

    public UnDoRedo(ActMain activity) {
        mActivityRef = new WeakReference<>(activity);
        mControls = new Control[2];
        mUndo = new ArrayDeque<>();
        mRedo = new ArrayDeque<>();
    }

    /**
     * Gets the Activity reference, or null if it has been garbage collected.
     * Callers should check for null before using.
     *
     * @return ActMain or null
     */
    private ActMain getActivity() {
        return mActivityRef.get();
    }

    /**
     * Sets the controls.
     *
     * @param containerUndo FrameLayout
     * @param viewUndo      ImageView
     * @param containerRedo FrameLayout
     * @param viewRedo      ImageView
     */
    public void setControls(final FrameLayout containerUndo,
                            final ImageView viewUndo,
                            final FrameLayout containerRedo,
                            final ImageView viewRedo) {
        mControls[CONTROL_UNDO] = new Control();
        mControls[CONTROL_UNDO].container = containerUndo;
        mControls[CONTROL_UNDO].img = viewUndo;
        mControls[CONTROL_UNDO].disable = R.drawable.ic_undo_disabled;
        mControls[CONTROL_UNDO].enable = R.drawable.ic_undo;
        mControls[CONTROL_REDO] = new Control();
        mControls[CONTROL_REDO].container = containerRedo;
        mControls[CONTROL_REDO].img = viewRedo;
        mControls[CONTROL_REDO].disable = R.drawable.ic_redo_disabled;
        mControls[CONTROL_REDO].enable = R.drawable.ic_redo;
    }

    /**
     * Tests if a change is detected.
     *
     * @return boolean
     */
    public boolean isChanged() {
        return mReferenceIndex != mUndo.size();
    }

    /**
     * Updates change index.
     */
    public void refreshChange() {
        mReferenceIndex = mUndo.size();
    }

    /**
     * Updates command.
     *
     * @param activity      MainActivity.
     * @param firstPosition The first position index.
     * @param refNbLines    The reference number of lines.
     * @param entries       The entries.
     * @return The command.
     */
    public ICommand insertInUnDoRedoForUpdate(final ActMain activity,
                                              final int firstPosition,
                                              final int refNbLines,
                                              List<LineEntry> entries) {
        ICommand cmd = new UpdateCommand(this,
                activity,
                firstPosition,
                refNbLines,
                entries);
        mUndo.push(cmd);
        manageControl(mControls[CONTROL_UNDO], true);
        manageControl(mControls[CONTROL_REDO], false);
        mRedo.clear();

        // Use the activity parameter passed to this method
        if (activity != null) {
            activity.refreshTitle();
        }
        return cmd;
    }

    /**
     * Updates and delete command.
     *
     * @param activity       MainActivity.
     * @param firstPosition  The first position index.
     * @param entriesUpdated Entries to be updated.
     * @param entriesDeleted Entries to be deleted.
     * @return The command.
     */
    public ICommand insertInUnDoRedoForUpdateAndDelete(final ActMain activity,
                                                       final int firstPosition,
                                                       List<LineEntry> entriesUpdated,
                                                       final Map<Integer, LineEntry> entriesDeleted) {
        ICommand cmd = new UpdateAndDeleteCommand(this,
                activity,
                firstPosition,
                entriesUpdated,
                entriesDeleted);
        mUndo.push(cmd);
        manageControl(mControls[CONTROL_UNDO], true);
        manageControl(mControls[CONTROL_REDO], false);
        mRedo.clear();

        // Use the activity parameter passed to this method
        if (activity != null) {
            activity.refreshTitle();
        }
        return cmd;
    }

    /**
     * Inserts delete command.
     *
     * @param activity MainActivity.
     * @param entries  The entries.
     * @return The command.
     */
    public ICommand insertInUnDoRedoForDelete(final ActMain activity,
                                              final Map<Integer, LineEntry> entries) {
        ICommand cmd = new DeleteCommand(activity, entries);
        mUndo.push(cmd);
        manageControl(mControls[CONTROL_UNDO], true);
        manageControl(mControls[CONTROL_REDO], false);
        mRedo.clear();

        // Use the activity parameter passed to this method
        if (activity != null) {
            activity.refreshTitle();
        }
        return cmd;
    }

    /**
     * Undo action
     */
    public void undo() {
        if (!mUndo.isEmpty()) {
            ICommand command = mUndo.pop();
            mRedo.push(command);
            command.unExecute();
            manageControl(mControls[CONTROL_REDO], true);
        }
        ActMain activity = getActivity();
        if (activity != null) {
            activity.refreshTitle();
        }
        manageControl(mControls[CONTROL_UNDO], !mUndo.isEmpty());
        if (!isChanged() && activity != null) {
            activity.getPayloadHex().resetUpdateStatus();
        }
    }

    /**
     * Redo action.
     */
    public void redo() {
        if (!mRedo.isEmpty()) {
            ICommand command = mRedo.pop();
            mUndo.push(command);
            command.execute();
            manageControl(mControls[CONTROL_UNDO], true);
        }
        ActMain activity = getActivity();
        if (activity != null) {
            activity.refreshTitle();
        }
        manageControl(mControls[CONTROL_REDO], !mRedo.isEmpty());
        if (!isChanged() && activity != null) {
            activity.getPayloadHex().resetUpdateStatus();
        }
    }

    /**
     * Clears the undo/redo stacks.
     */
    public void clear() {
        for (Control ctrl : mControls)
            manageControl(ctrl, false);
        mUndo.clear();
        mRedo.clear();
        mReferenceIndex = 0;
        ActMain activity = getActivity();
        if (activity != null) {
            activity.refreshTitle();
        }
    }

    /**
     * Cleanup method to prevent memory leaks.
     * Clears all View references held by the controls to break circular references.
     * MUST be called when the Activity is destroyed.
     */
    public void cleanup() {
        // Clear View references to break circular reference with MainPopupWindow
        if (mControls != null) {
            for (Control ctrl : mControls) {
                if (ctrl != null) {
                    ctrl.container = null;
                    ctrl.img = null;
                }
            }
        }
        // Clear command stacks
        mUndo.clear();
        mRedo.clear();
    }

    /**
     * Manages control state.
     *
     * @param control The control.
     * @param enabled Enabled ?
     */
    private void manageControl(final Control control, final boolean enabled) {
        if (control != null && control.img != null) {
            if (control.container != null)
                control.container.setEnabled(enabled);
            ActMain activity = getActivity();
            if (activity != null) {
                control.img.setImageDrawable(ContextCompat.getDrawable(activity, enabled ? control.enable : control.disable));
                control.img.setEnabled(enabled);
            }
        }
    }

    private static class Control {
        private FrameLayout container;
        private ImageView img;
        private int enable;
        private int disable;
    }
}
