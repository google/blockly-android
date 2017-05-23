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

    public int size() {
        return mMap.size();
    }

    public void clear() {
        mMap.clear();
    }

    public boolean contains(Object o) {
        return mMap.containsKey(o);
    }

    public boolean add(E e) {
        return mMap.put(e, e) == null;
    }


    public E getAt(int i) {
        return mMap.keyAt(i);
    }

    public boolean remove(Object o) {
        return mMap.remove(o) != null;
    }

    public E removeAt(int i) {
        return removeAt(i);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleArraySet<?> other = (SimpleArraySet<?>) o;
        return mMap.equals(other.mMap);
    }

    @Override
    public int hashCode() {
        return mMap.hashCode();
    }
}
