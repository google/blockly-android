package com.google.blockly.ui.fieldview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.google.blockly.MainActivity;
import com.google.blockly.model.Field;
import com.google.blockly.ui.WorkspaceHelper;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link FieldColourView}.
 */
public class FieldColourViewTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static final int LAYOUT_HEIGHT = 1000;
    private static final int LAYOUT_WIDTH = 1000;

    private RelativeLayout mLayout;
    private FieldColourView mFieldColorView;

    // Cannot mock final classes.
    private Field.FieldColour mFieldColour;
    private MainActivity mActivity;

    @Mock
    WorkspaceHelper mMockWorkspaceHelper;

    public FieldColourViewTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mActivity = getActivity();

        mLayout = new RelativeLayout(mActivity);
        mLayout.layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);

        mFieldColour = new Field.FieldColour("FieldColour");
        mFieldColorView = new FieldColourView(mActivity, mFieldColour, mMockWorkspaceHelper);
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

        // Reset colour before test.
        mFieldColour.setColour(0);
        assertEquals(0, mFieldColour.getColour());

        // Simulate click on the color panel.
        popupWindowContentView.onTouchEvent(
                MotionEvent.obtain(0 /* downTime */, 0 /* eventTime */, MotionEvent.ACTION_DOWN,
                        0f /* x */, 0f /* y */, 0 /* metaState */));

        // Verify both field and field view background have been set to correct color.
        assertEquals(0xffffff, mFieldColour.getColour());  // setColour() masks out alpha.
        assertEquals(0xffffffff, ((ColorDrawable) mFieldColorView.getBackground()).getColor());

        // Popup window should have disappeared.
        assertFalse(popupWindow.isShowing());
    }
}
