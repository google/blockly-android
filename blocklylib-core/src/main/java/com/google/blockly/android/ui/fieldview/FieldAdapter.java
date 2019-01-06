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

class FieldAdapter<T> extends ArrayAdapter<T> {
    private AppCompatSpinner mSpinner;

    public FieldAdapter(@NonNull Context context, int resource, @NonNull List<T> objects, AppCompatSpinner mSpinner) {
        super(context, resource, objects);
        this.mSpinner = mSpinner;
    }

    public FieldAdapter(@NonNull Context context, int resource, AppCompatSpinner mSpinner) {
        super(context, resource);
        this.mSpinner = mSpinner;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        StringWriter stackTrace = new StringWriter();
        PrintWriter printer = new PrintWriter(stackTrace);
        new RuntimeException().printStackTrace(printer);
        if (stackTrace.toString().contains("at android.widget.Spinner.measureContentWidth")) {
            return super.getView(mSpinner.getSelectedItemPosition(), convertView, parent);
        } else {
            return super.getView(position, convertView, parent);
        }
    }
}