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
