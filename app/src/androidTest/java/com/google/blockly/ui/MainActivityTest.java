package com.google.blockly.ui;

import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.test.ActivityInstrumentationTestCase2;

import com.google.blockly.MainActivity;

/**
 * Test {@link MainActivity} lifecycle events.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity mActivity;
    private Instrumentation mInstrumentation;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() {
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
    }

    // Test switching around device orientation to make sure there are no crashes.
    public void testSwitchDeviceOrientation() {
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mInstrumentation.waitForIdleSync();
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mInstrumentation.waitForIdleSync();
        // Direct switch from one orientation to its reverse appears to be handled differently from
        // switching between portrait and landscape or vice versa - so make sure we cover this case.
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        mInstrumentation.waitForIdleSync();
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mInstrumentation.waitForIdleSync();
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        mInstrumentation.waitForIdleSync();
    }
}
