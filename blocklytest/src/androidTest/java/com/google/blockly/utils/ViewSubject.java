/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import android.view.View;
import android.view.ViewParent;

import com.google.blockly.android.ui.BlockView;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;

/**
 * Fluent subject for {@link View} objects.
 *
 * <code>assertAbout(ViewSubject.view()).that(testView).is...</code>
 */
public class ViewSubject extends Subject<ViewSubject, View> {
    private static final SubjectFactory<ViewSubject, View> FACTORY =
            new SubjectFactory<ViewSubject, View>() {
                @Override
                public ViewSubject getSubject(FailureStrategy fs, View target) {
                    return new ViewSubject(fs, target);
                }
            };

    public static SubjectFactory<ViewSubject, View> view() {
        return FACTORY;
    }

    // Must be public and non-final for generated subclasses (wrappers)
    public ViewSubject(FailureStrategy failureStrategy, View subject) {
        super(failureStrategy, subject);
    }

    /**
     * Tests whether this subject is a descendant, directly or indirectly, of {@code ancestor}.
     *
     * @param ancestor The expected ancestor in the view tree.
     */
    public void isDescendantOf(ViewParent ancestor) {
        View subject = actual();

        ViewParent parent = subject.getParent();
        while (parent != null) {
            if (ancestor == parent) {
                return;  // Success
            }
            parent = parent.getParent();
        }
        fail("is descendant of", ancestor);
    }

    // Convenience cast
    public void isDescendantOf(BlockView ancestor) {
        isDescendantOf((ViewParent) ancestor);
    }
}
