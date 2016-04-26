/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui.fieldview;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.test.ActivityInstrumentationTestCase2;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.google.blockly.android.BlocklyTestActivity;
import com.google.blockly.model.FieldColor;

import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link BasicFieldColorView}.
 */
public class BasicFieldColorViewTest
        extends ActivityInstrumentationTestCase2<BlocklyTestActivity> {

    private static final int LAYOUT_HEIGHT = 1000;
    private static final int LAYOUT_WIDTH = 1000;

    private RelativeLayout mLayout;
    private BasicFieldColorView mFieldColorView;

    // Cannot mock final classes.
    private FieldColor mFieldColor;
    private BlocklyTestActivity mActivity;

    public BasicFieldColorViewTest() {
        super(BlocklyTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mActivity = getActivity();

        mLayout = new RelativeLayout(mActivity);
        mLayout.layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);

        mFieldColor = new FieldColor("FieldColor");
        mFieldColorView = new BasicFieldColorView(mActivity);
        mFieldColorView.setField(mFieldColor);
        mLayout.addView(mFieldColorView);
    }

    // Verify that clicking on the field view opens popup window.
    public void testPopupWindowProperties() {
        mFieldColorView.performClick();
        final PopupWindow popupWindow = mFieldColorView.getColorPopupWindow();

        assertNotNull(popupWindow);
        assertTrue(popupWindow.isShowing());
        assertTrue(popupWindow.isTouchable());
        assertTrue(popupWindow.isFocusable());
    }

    // Verify that clicking on popup window selects a color.
    public void testPopupWindowChangeColor() {
        mFieldColorView.performClick();
        final PopupWindow popupWindow = mFieldColorView.getColorPopupWindow();
        final View popupWindowContentView = popupWindow.getContentView();
        assertNotNull(popupWindowContentView);

        // Reset color before test.
        mFieldColor.setColor(0);
        assertEquals(0, mFieldColor.getColor());

        // Simulate click on the color panel.
        popupWindowContentView.onTouchEvent(
                MotionEvent.obtain(0 /* downTime */, 0 /* eventTime */, MotionEvent.ACTION_DOWN,
                        0f /* x */, 0f /* y */, 0 /* metaState */));

        // Verify both field and field view background have been set to correct color.
        final int expectedColour = 0xffffff;
        assertEquals(expectedColour, mFieldColor.getColor());  // setColour() masks out alpha.
        assertEquals(BasicFieldColorView.ALPHA_OPAQUE | expectedColour,
                ((ColorDrawable) mFieldColorView.getBackground()).getColor());

        // Popup window should have disappeared.
        assertFalse(popupWindow.isShowing());
    }

    // Verify that changing color in the field updates the UI.
    public void testFieldUpdatesView() {
        mFieldColor.setColor(0);
        assertEquals(BasicFieldColorView.ALPHA_OPAQUE,
                ((ColorDrawable)mFieldColorView.getBackground()).getColor());

        mFieldColor.setColor(Color.RED);
        assertEquals(BasicFieldColorView.ALPHA_OPAQUE | Color.RED,
                ((ColorDrawable)mFieldColorView.getBackground()).getColor());

        mFieldColor.setColor(Color.GREEN);
        assertEquals(BasicFieldColorView.ALPHA_OPAQUE | Color.GREEN,
                ((ColorDrawable)mFieldColorView.getBackground()).getColor());

        mFieldColor.setColor(Color.BLUE);
        assertEquals(BasicFieldColorView.ALPHA_OPAQUE | Color.BLUE,
                ((ColorDrawable)mFieldColorView.getBackground()).getColor());

        mFieldColor.setColor(Color.WHITE);
        assertEquals(BasicFieldColorView.ALPHA_OPAQUE | Color.WHITE,
                ((ColorDrawable)mFieldColorView.getBackground()).getColor());
    }
}
