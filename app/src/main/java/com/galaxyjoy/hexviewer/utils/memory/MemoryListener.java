package com.galaxyjoy.hexviewer.utils.memory;

/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Memory listener.
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
public interface MemoryListener {
  void onLowAppMemory(boolean disabled, MemoryInfo mi);
}
