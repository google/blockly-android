package com.google.blockly.ui.fieldview;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.test.ActivityInstrumentationTestCase2;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.google.blockly.BlocklyTestActivity;
import com.google.blockly.model.Field;
import com.google.blockly.ui.WorkspaceHelper;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link FieldColourView}.
 */
public class FieldColourViewTest extends ActivityInstrumentationTestCase2<BlocklyTestActivity> {

    private static final int LAYOUT_HEIGHT = 1000;
    private static final int LAYOUT_WIDTH = 1000;

    private RelativeLayout mLayout;
    private FieldColourView mFieldColorView;

    // Cannot mock final classes.
    private Field.FieldColour mFieldColour;
    private BlocklyTestActivity mActivity;

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    public FieldColourViewTest() {
        super(BlocklyTestActivity.class);
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
        final int expectedColour = 0xffffff;
        assertEquals(expectedColour, mFieldColour.getColour());  // setColour() masks out alpha.
        assertEquals(FieldColourView.ALPHA_OPAQUE | expectedColour,
                ((ColorDrawable) mFieldColorView.getBackground()).getColor());

        // Popup window should have disappeared.
        assertFalse(popupWindow.isShowing());
    }

    // Verify that changing colour in the field updates the UI.
    public void testFieldUpdatesView() {
        mFieldColour.setColour(0);
        assertEquals(FieldColourView.ALPHA_OPAQUE,
                ((ColorDrawable) mFieldColorView.getBackground()).getColor());

        mFieldColour.setColour(Color.RED);
        assertEquals(FieldColourView.ALPHA_OPAQUE | Color.RED,
                ((ColorDrawable) mFieldColorView.getBackground()).getColor());

        mFieldColour.setColour(Color.GREEN);
        assertEquals(FieldColourView.ALPHA_OPAQUE | Color.GREEN,
                ((ColorDrawable) mFieldColorView.getBackground()).getColor());

        mFieldColour.setColour(Color.BLUE);
        assertEquals(FieldColourView.ALPHA_OPAQUE | Color.BLUE,
                ((ColorDrawable) mFieldColorView.getBackground()).getColor());

        mFieldColour.setColour(Color.WHITE);
        assertEquals(FieldColourView.ALPHA_OPAQUE | Color.WHITE,
                ((ColorDrawable) mFieldColorView.getBackground()).getColor());
    }
}
