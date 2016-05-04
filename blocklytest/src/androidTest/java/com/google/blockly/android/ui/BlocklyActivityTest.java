/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.blockly.android.ui;

import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ImageButton;

import com.google.blockly.android.BlocklyTestActivity;
import com.google.blockly.android.R;

/**
 * Test Activity lifecycle events using {@link BlocklyTestActivity}.
 */
public class BlocklyActivityTest extends ActivityInstrumentationTestCase2<BlocklyTestActivity> {
    private BlocklyTestActivity mActivity;
    private Instrumentation mInstrumentation;

    public BlocklyActivityTest() {
        super(BlocklyTestActivity.class);
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
        mInstrumentation.waitForIdleSync();
        final WorkspaceView workspaceView = (WorkspaceView) mActivity.findViewById(R.id.workspace);
        final VirtualWorkspaceView virtualWorkspaceView =
                (VirtualWorkspaceView) workspaceView.getParent();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(1.0f, virtualWorkspaceView.getViewScale(), 1e-5);
                mActivity.getController().zoomIn();
                assertTrue(virtualWorkspaceView.getViewScale() > 1.0f);
            }
        });

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getController().zoomOut();
                assertEquals(1.0f, virtualWorkspaceView.getViewScale(), 1e-5);
            }
        });

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getController().zoomOut();
                assertTrue(virtualWorkspaceView.getViewScale() < 1.0f);
            }
        });

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getController().recenterWorkspace();
                assertEquals(1.0f, virtualWorkspaceView.getViewScale(), 1e-5);
            }
        });
    }
}
