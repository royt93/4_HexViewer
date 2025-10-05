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
            // Return copy to avoid concurrent modification
            return new ArrayList<>(buffer).iterator();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        lock.lock();
        try {
            return buffer.toArray();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        lock.lock();
        try {
            return buffer.toArray(a);
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
            return new ArrayList<>(buffer);
        } finally {
            lock.unlock();
        }
    }
}
