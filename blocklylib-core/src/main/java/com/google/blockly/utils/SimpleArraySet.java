package com.google.blockly.utils;

import android.support.v4.util.SimpleArrayMap;

/**
 * Set-like wrapper around SimpleArrayMap.
 */
public final class SimpleArraySet<E> {
    private final SimpleArrayMap<E, Object> mMap;

    public SimpleArraySet() {
        this.mMap = new SimpleArrayMap<>();
    }

    public SimpleArraySet(int initialCapacity) {
        this.mMap = new SimpleArrayMap<>(initialCapacity);
    }

    public boolean add(E e) {
        return mMap.put(e, e) == null;
    }
}
