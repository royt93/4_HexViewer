package com.galaxyjoy.hexviewer.util.memory;

public interface MemoryListener {
    void onLowAppMemory(boolean disabled, MemoryInfo mi);
}
