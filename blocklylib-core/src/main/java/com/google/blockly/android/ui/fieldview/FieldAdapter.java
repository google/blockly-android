package com.google.blockly.android.ui.fieldview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSpinner;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Common Code for Field DropDown Adapters
 * This makes a Spinner's width, the width of the currently selected item, not a one size fits all.
 */
class FieldAdapter<T> extends ArrayAdapter<T> {
    private AppCompatSpinner mSpinner;

    public FieldAdapter(@NonNull Context context, int resource, @NonNull List<T> objects, @Nullable AppCompatSpinner mSpinner) {
        super(context, resource, objects);
        this.mSpinner = mSpinner;
    }

    public FieldAdapter(@NonNull Context context, int resource, @Nullable AppCompatSpinner mSpinner) {
        super(context, resource);
        this.mSpinner = mSpinner;
    }

    private boolean isMeasuringContentWidth() {
        if (mSpinner != null) {
            // Generate Stack Trace
            StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
            // Use Stack Trace To Check if Method is being Called By android.widget.Spinner.measureContentWidth (fixes #735)
            for (int i = 0; i < stackTrace.length; i++) {
                if (stackTrace[i].toString().contains("android.widget.Spinner.measureContentWidth")) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (isMeasuringContentWidth()) {
            return super.getView(mSpinner.getSelectedItemPosition(), convertView, parent);
        } else {
            return super.getView(position, convertView, parent);
        }
    }
}
