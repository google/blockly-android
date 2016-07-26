package com.google.blockly.android.ui.fieldview;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class VariableRequestCallback {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REQUEST_DELETE, REQUEST_RENAME, REQUEST_CREATE})
    public @interface VariableRequestType {
    }

    public static final int REQUEST_DELETE = 1;
    public static final int REQUEST_RENAME = 2;
    public static final int REQUEST_CREATE = 3;

    public abstract void onVariableRequest(int request, String variable);
}
