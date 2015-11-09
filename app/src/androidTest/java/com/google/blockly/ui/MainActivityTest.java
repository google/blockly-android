package com.google.blockly.ui;

import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ImageButton;

import com.google.blockly.MainActivity;
import com.google.blockly.R;

/**
 * Test {@link MainActivity} lifecycle events and global view operations.
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

    // Test zooming into workspace, then out, then reset.
    public void testZoomInOutReset() {
        final VirtualWorkspaceView virtualWorkspaceView =
                (VirtualWorkspaceView) mActivity.findViewById(R.id.virtual_workspace);
        final ImageButton zoomInButton = (ImageButton) mActivity.findViewById(R.id.zoom_in_button);
        final ImageButton zoomOutButton =
                (ImageButton) mActivity.findViewById(R.id.zoom_out_button);
        final ImageButton resetViewButton =
                (ImageButton) mActivity.findViewById(R.id.reset_view_button);

        assertEquals(1.0, virtualWorkspaceView.getViewScale(), 1e-5);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zoomInButton.performClick();
                assertTrue(virtualWorkspaceView.getViewScale() > 1.0f);
            }
        });

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zoomOutButton.performClick();
                assertEquals(1.0, virtualWorkspaceView.getViewScale(), 1e-5);
            }
        });

        assertEquals(1.0, virtualWorkspaceView.getViewScale(), 1e-5);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zoomOutButton.performClick();
                assertTrue(virtualWorkspaceView.getViewScale() < 1.0f);
            }
        });

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resetViewButton.performClick();
                assertEquals(1.0, virtualWorkspaceView.getViewScale(), 1e-5);
            }
        });
    }
}
