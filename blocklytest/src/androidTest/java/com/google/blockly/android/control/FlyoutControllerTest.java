/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.control;

import android.support.test.rule.ActivityTestRule;

import com.google.blockly.android.BlocklyTestActivity;
import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.CategorySelectorFragment;
import com.google.blockly.android.FlyoutFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class FlyoutControllerTest extends BlocklyTestCase {

    private FlyoutController mFlyoutController;
    private FlyoutFragment mMockToolboxFlyout;
    private FlyoutFragment mMockTrashFlyout;

    private BlocklyController mController;
    private CategorySelectorFragment mMockCategoryFragment;
    private BlocklyTestActivity mActivity;

    @Rule
    public final ActivityTestRule<BlocklyTestActivity> mActivityRule =
        new ActivityTestRule<>(BlocklyTestActivity.class);

    @Before
    public void setUp() throws Exception {
        configureForUIThread();
        mActivity = mActivityRule.getActivity();
        mController = mActivity.getController();
        mFlyoutController = mController.mFlyoutController;

        mMockToolboxFlyout = mock(FlyoutFragment.class);
        mMockCategoryFragment = mock(CategorySelectorFragment.class);
        mMockTrashFlyout = mock(FlyoutFragment.class);

        mController.setToolboxUi(mMockToolboxFlyout, mMockCategoryFragment);
        mController.setTrashUi(mMockTrashFlyout);
    }

    // TODO: test FlyoutController
    @Test
    public void testSetup() {
        // Validates that setUp() runs and does not throw anything. Not a good test, but something.
    }
}
