package com.galaxyjoy.hexviewer.ui.undoredo.commands;

import android.util.Log;

import java.util.List;
import java.util.Map;

import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.ui.activities.MainActivity;
import com.galaxyjoy.hexviewer.ui.undoredo.ICommand;
import com.galaxyjoy.hexviewer.ui.undoredo.UnDoRedo;

public class UpdateAndDeleteCommand implements ICommand {
    private final DeleteCommand mDelete;
    private final UpdateCommand mUpdate;


    public UpdateAndDeleteCommand(final UnDoRedo undoRedo,
                                  final MainActivity activity,
                                  final int firstPosition,
                                  List<LineEntry> entriesUpdated,
                                  final Map<Integer, LineEntry> entriesDeleted) {
        mUpdate = new UpdateCommand(undoRedo,
                activity,
                firstPosition,
                entriesUpdated.size(),
                entriesUpdated);
        mDelete = new DeleteCommand(activity,
                entriesDeleted);
    }

    /**
     * Execute the command.
     */
    public void execute() {
        Log.i(getClass().getName(), "execute");
        mDelete.execute();
        mUpdate.execute();
    }

    /**
     * Un-Execute the command.
     */
    public void unExecute() {
        Log.i(getClass().getName(), "unExecute");
        mUpdate.unExecute();
        mDelete.unExecute();
    }
}
