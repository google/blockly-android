package com.google.blockly.android.ui.fieldview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSpinner;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;
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

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Generate Stack Trace
        StringWriter stackTrace = new StringWriter();
        PrintWriter printer = new PrintWriter(stackTrace);
        new RuntimeException().printStackTrace(printer);
        // Use Stack Trace To Check if Method is being Called By android.widget.Spinner.measureContentWidth (fixes #735)
        if (mSpinner != null && stackTrace.toString().contains("at android.widget.Spinner.measureContentWidth")) {
            return super.getView(mSpinner.getSelectedItemPosition(), convertView, parent);
        } else {
            return super.getView(position, convertView, parent);
        }
    }
}
