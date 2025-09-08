/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Undo Redo command
 * </p>
 *
 * @author Keidan
 * <p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.util.memory;

public interface MemoryListener {
    void onLowAppMemory(boolean disabled, MemoryInfo mi);
}
