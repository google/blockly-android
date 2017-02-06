package com.google.blockly.android;

import android.support.test.rule.ActivityTestRule;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.FlyoutController;

import org.junit.Before;
import org.junit.Rule;

import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FlyoutControllerTest extends BlocklyTestCase {

    private BlocklyController mMockController;
    private FlyoutController mFlyoutController;
    private FlyoutFragment mToolboxFlyout;
    private FlyoutFragment mTrashFlyout;
    private CategorySelectorFragment mCategoryFragment;
    private BlocklyTestActivity mActivity;

    @Rule
    public final ActivityTestRule<BlocklyTestActivity> mActivityRule =
        new ActivityTestRule<>(BlocklyTestActivity.class);

    @Before
    public void setUp() throws Exception {
        configureForUIThread();
        mMockController = mock(BlocklyController.class);
        mFlyoutController = new FlyoutController(mMockController);

        mToolboxFlyout = mock(FlyoutFragment.class);
        mCategoryFragment = mock(CategorySelectorFragment.class);
        mTrashFlyout = mock(FlyoutFragment.class);
        mFlyoutController.setToolboxUiComponents(mCategoryFragment, mToolboxFlyout);
        mFlyoutController.setTrashUi(mTrashFlyout);
    }

    // TODO: test FlyoutController
}
