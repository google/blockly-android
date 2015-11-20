/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.google.blockly.ui;

import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ImageButton;

import com.google.blockly.BlocklySectionsActivity;
import com.google.blockly.R;

/**
 * Test {@link BlocklySectionsActivity} lifecycle events.
 */
public class BlocklyActivityTest extends ActivityInstrumentationTestCase2<BlocklySectionsActivity> {
    private BlocklySectionsActivity mActivity;
    private Instrumentation mInstrumentation;

    public BlocklyActivityTest() {
        super(BlocklySectionsActivity.class);
    }

    @Override
    public void setUp() {
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
    }

    // Test switching around device orientation to make sure there are no crashes.
    public void testSwitchDeviceOrientation() {
        mInstrumentation.waitForIdleSync();
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

        assertEquals(1.0f, virtualWorkspaceView.getViewScale(), 1e-5);
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
                assertEquals(1.0f, virtualWorkspaceView.getViewScale(), 1e-5);
            }
        });

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
                assertEquals(1.0f, virtualWorkspaceView.getViewScale(), 1e-5);
            }
        });
    }
}
