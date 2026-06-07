/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Lightweight circular log buffer - replaces Apache Commons CircularFifoQueue
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe circular buffer for logging
 * Lightweight replacement for Apache Commons CircularFifoQueue
 * Automatically evicts oldest entries when capacity is reached
 *
 * Saves ~600KB by removing Apache Commons dependency
 */
public class CircularLogBuffer implements Queue<String> {
    private final ArrayDeque<String> buffer;
    private final int capacity;
    private final Lock lock = new ReentrantLock();

    public CircularLogBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new ArrayDeque<>(capacity);
    }

    @Override
    public boolean add(String message) {
        lock.lock();
        try {
            // Remove oldest if at capacity
            if (buffer.size() >= capacity) {
                buffer.removeFirst();
            }
            return buffer.add(message);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(String message) {
        return add(message);
    }

    @Override
    public String remove() {
        lock.lock();
        try {
            return buffer.remove();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String poll() {
        lock.lock();
        try {
            return buffer.poll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String element() {
        lock.lock();
        try {
            return buffer.element();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String peek() {
        lock.lock();
        try {
            return buffer.peek();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return buffer.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return buffer.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            buffer.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        lock.lock();
        try {
            return buffer.contains(o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public java.util.Iterator<String> iterator() {
        lock.lock();
        try {
            // Snapshot the buffer to avoid concurrent modification.
            // Guard against OOM on MediaTek devices (FrameIdentify monitors ArrayList.iterator)
            // If snapshot fails, return empty iterator instead of throwing OOM.
            if (buffer.isEmpty()) {
                return java.util.Collections.<String>emptyList().iterator();
            }
            try {
                String[] snapshot = buffer.toArray(new String[0]);
                return java.util.Arrays.asList(snapshot).iterator();
            } catch (OutOfMemoryError oom) {
                // Free log entries immediately and return empty iterator
                buffer.clear();
                return java.util.Collections.<String>emptyList().iterator();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        lock.lock();
        try {
            try {
                return buffer.toArray();
            } catch (OutOfMemoryError oom) {
                return new Object[0];
            }
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        lock.lock();
        try {
            try {
                return buffer.toArray(a);
            } catch (OutOfMemoryError oom) {
                return (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), 0);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        lock.lock();
        try {
            return buffer.remove(o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsAll(java.util.Collection<?> c) {
        lock.lock();
        try {
            return buffer.containsAll(c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addAll(java.util.Collection<? extends String> c) {
        lock.lock();
        try {
            for (String s : c) {
                add(s);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeAll(java.util.Collection<?> c) {
        lock.lock();
        try {
            return buffer.removeAll(c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean retainAll(java.util.Collection<?> c) {
        lock.lock();
        try {
            return buffer.retainAll(c);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get all log entries as an immutable list
     */
    public List<String> getAll() {
        lock.lock();
        try {
            try {
                return new ArrayList<>(buffer);
            } catch (OutOfMemoryError oom) {
                return java.util.Collections.emptyList();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Aggressively shrink the buffer to free memory under pressure.
     * Keeps only the most recent {@code keepCount} entries.
     * Called by onTrimMemory/onLowMemory when TRIM_MEMORY_COMPLETE is received.
     *
     * @param keepCount Number of most-recent entries to retain (0 = clear all).
     */
    public void shrink(int keepCount) {
        lock.lock();
        try {
            if (keepCount <= 0 || buffer.size() <= keepCount) {
                if (keepCount <= 0) buffer.clear();
                return;
            }
            // Remove oldest entries until only keepCount remain
            while (buffer.size() > keepCount) {
                buffer.removeFirst();
            }
        } finally {
            lock.unlock();
        }
    }
}
