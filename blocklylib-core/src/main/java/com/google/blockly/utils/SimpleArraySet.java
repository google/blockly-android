/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    public boolean isEmpty() {
        return mMap.isEmpty();
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
