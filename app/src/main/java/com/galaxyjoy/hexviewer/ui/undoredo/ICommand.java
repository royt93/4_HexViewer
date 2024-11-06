package com.galaxyjoy.hexviewer.ui.undoredo;

public interface ICommand {
    /**
     * Execute the command.
     */
    void execute();

    /**
     * Un-Execute the command.
     */
    void unExecute();
}
