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
import android.support.test.rule.ActivityTestRule;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.google.blockly.android.BlocklyTestActivity;
import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.model.FieldColor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link BasicFieldColorView}.
 */
public class BasicFieldColorViewTest extends BlocklyTestCase {

    private static final int LAYOUT_HEIGHT = 1000;
    private static final int LAYOUT_WIDTH = 1000;

    private RelativeLayout mLayout;
    private BasicFieldColorView mFieldColorView;

    // Cannot mock final classes.
    private FieldColor mFieldColor;
    private BlocklyTestActivity mActivity;

    @Rule
    public final ActivityTestRule<BlocklyTestActivity> mActivityRule =
        new ActivityTestRule<>(BlocklyTestActivity.class);

    @Before
    public void setUp() throws Exception {
        configureForUIThread();
        mActivity = mActivityRule.getActivity();

        mLayout = new RelativeLayout(mActivity);
        mLayout.layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);

        mFieldColor = new FieldColor("FieldColor");
        mFieldColorView = new BasicFieldColorView(mActivity);
        mFieldColorView.setField(mFieldColor);
        mLayout.addView(mFieldColorView);
    }

    // Verify that clicking on the field view opens popup window.
    @Test
    public void testPopupWindowProperties() {
        mFieldColorView.performClick();
        final PopupWindow popupWindow = mFieldColorView.getColorPopupWindow();

        assertThat(popupWindow).isNotNull();
        assertThat(popupWindow.isShowing()).isTrue();
        assertThat(popupWindow.isTouchable()).isTrue();
        assertThat(popupWindow.isFocusable()).isTrue();
    }

    // Verify that clicking on popup window selects a color.
    @Test
    public void testPopupWindowChangeColor() {
        mFieldColorView.performClick();
        final PopupWindow popupWindow = mFieldColorView.getColorPopupWindow();
        final View popupWindowContentView = popupWindow.getContentView();
        assertThat(popupWindowContentView).isNotNull();

        // Reset color before test.
        mFieldColor.setColor(0);
        assertThat(mFieldColor.getColor()).isEqualTo(0);

        // Simulate click on the color panel.
        popupWindowContentView.onTouchEvent(
                MotionEvent.obtain(0 /* downTime */, 0 /* eventTime */, MotionEvent.ACTION_DOWN,
                        0f /* x */, 0f /* y */, 0 /* metaState */));

        // Verify both field and field view background have been set to correct color.
        final int expectedColour = 0xffffff;
        assertThat(mFieldColor.getColor())
                .isEqualTo(expectedColour);  // setColour() masks out alpha.
        assertThat(((ColorDrawable) mFieldColorView.getBackground()).getColor())
                .isEqualTo(BasicFieldColorView.ALPHA_OPAQUE | expectedColour);

        // Popup window should have disappeared.
        assertThat(popupWindow.isShowing()).isFalse();
    }

    // Verify that changing color in the field updates the UI.
    @Test
    public void testFieldUpdatesView() {
        mFieldColor.setColor(0);
        assertThat(((ColorDrawable)mFieldColorView.getBackground()).getColor())
                .isEqualTo(BasicFieldColorView.ALPHA_OPAQUE);

        mFieldColor.setColor(Color.RED);
        assertThat(((ColorDrawable)mFieldColorView.getBackground()).getColor())
                .isEqualTo(BasicFieldColorView.ALPHA_OPAQUE | Color.RED);

        mFieldColor.setColor(Color.GREEN);
        assertThat(((ColorDrawable)mFieldColorView.getBackground()).getColor())
                .isEqualTo(BasicFieldColorView.ALPHA_OPAQUE | Color.GREEN);

        mFieldColor.setColor(Color.BLUE);
        assertThat(((ColorDrawable)mFieldColorView.getBackground()).getColor())
                .isEqualTo(BasicFieldColorView.ALPHA_OPAQUE | Color.BLUE);

        mFieldColor.setColor(Color.WHITE);
        assertThat(((ColorDrawable)mFieldColorView.getBackground()).getColor())
                .isEqualTo(BasicFieldColorView.ALPHA_OPAQUE | Color.WHITE);
    }
}
