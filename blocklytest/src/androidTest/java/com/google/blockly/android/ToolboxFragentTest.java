package com.google.blockly.android;

import android.support.test.rule.ActivityTestRule;

import com.google.blockly.android.test.*;
import com.google.blockly.android.test.R;
import com.google.blockly.android.ui.NameVariableDialog;
import com.google.blockly.model.ToolboxCategory;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ToolboxFragentTest extends BlocklyTestCase {

    private ToolboxFragment mToolboxFragment;
    private BlocklyTestActivity mActivity;

    @Rule
    public final ActivityTestRule<BlocklyTestActivity> mActivityRule =
        new ActivityTestRule<>(BlocklyTestActivity.class);

    @Before
    public void setUp() throws Exception {
        configureForUIThread();
        mToolboxFragment = new ToolboxFragment();

        mActivity = mActivityRule.getActivity();
        mActivity.getSupportFragmentManager().beginTransaction().add(mToolboxFragment, "Toolbox").commit();
    }

    @Test
    public void testTrashClosedWhenToolboxOpened() {
        TrashFragment trashMock = mock(TrashFragment.class);

        mToolboxFragment.setTrashFragment(trashMock);
        mToolboxFragment.setCurrentCategory(mock(ToolboxCategory.class));

        onView(withId(R.id.blockly_toolbox));
        verify(trashMock, times(1)).setOpened(false);
    }
}
