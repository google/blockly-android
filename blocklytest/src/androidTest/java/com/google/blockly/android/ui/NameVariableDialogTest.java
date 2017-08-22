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
package com.google.blockly.android.ui;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.google.blockly.android.BlocklyTestActivity;
import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.test.R;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
public class NameVariableDialogTest extends BlocklyTestCase {

  private NameVariableDialog mNameVariableDialogFragment;
  private BlocklyTestActivity mActivity;

  @Rule
  public final ActivityTestRule<BlocklyTestActivity> mActivityRule =
      new ActivityTestRule<>(BlocklyTestActivity.class);

  @Before
  public void setUp() throws Exception {
    configureForUIThread();
    mNameVariableDialogFragment = new NameVariableDialog();

    mActivity = mActivityRule.getActivity();
  }

  @Test
  public void dialogShowsOldVariableNameWhenRenaming() throws Exception {
    testTimeoutMs *= 5;  // Allow longer time for dialog interaction test

    String variableName = "oldVariableName";

    mNameVariableDialogFragment.setVariable(
            variableName, mock(NameVariableDialog.Callback.class), true);
    mNameVariableDialogFragment.show(mActivity.getSupportFragmentManager(), "RenameFragment");
    onView(withId(R.id.description)).check(matches(withText(new StringContains(variableName))));
  }

  @Test
  public void dialogShowsGenericTextForNewVariable() throws Exception {
    testTimeoutMs *= 5;  // Allow longer time for dialog interaction test

    mNameVariableDialogFragment.setVariable("", mock(NameVariableDialog.Callback.class), false);
    mNameVariableDialogFragment.show(mActivity.getSupportFragmentManager(), "CreateFragment");
    onView(withId(R.id.description)).check(matches(withText("New variable name")));
  }
}
