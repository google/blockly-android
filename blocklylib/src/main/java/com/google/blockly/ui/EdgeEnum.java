package com.google.blockly.ui;

import android.support.annotation.IntDef;
import android.support.v4.view.GravityCompat;
import android.view.Gravity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Subset of Gravity to identify the edge the category tabs should be bound to.
 */
@IntDef(flag=true, value={
        Gravity.TOP,
        Gravity.LEFT,
        Gravity.BOTTOM,
        Gravity.RIGHT,
        GravityCompat.START,
        GravityCompat.END
})
@Retention(RetentionPolicy.SOURCE)
public @interface EdgeEnum {}
