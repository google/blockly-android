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

import android.app.Dialog;
import android.os.Bundle;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.TextView;

import com.google.blockly.android.BlocklyTestActivity;
import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.test.R;
import com.google.blockly.utils.LangUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
public class NameVariableDialogTest extends BlocklyTestCase {

    @Rule
    public final ActivityTestRule<BlocklyTestActivity> mActivityRule = new ActivityTestRule<>(
            BlocklyTestActivity.class);

    private NameVariableDialog mNameVariableDialogFragment;
    private BlocklyTestActivity mActivity;

    private CountDownLatch latch;
    private String description;

    public void setDescription(String description) {
        this.description = description;
    }

    public void countDown() {
        latch.countDown();
    }

    /**
     * Custom Dialog that calls a callback when R.id.description is set.
     */
    public static class TestDialog extends NameVariableDialog {
        private NameVariableDialogTest mTest;
        public TestDialog(NameVariableDialogTest mTest) {
            super();
            this.mTest = mTest;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceBundle) {
            Dialog dialog = super.onCreateDialog(savedInstanceBundle);

            mTest.setDescription(((TextView) nameView.findViewById(R.id.description)).getText().toString());
            mTest.countDown();

            return dialog;
        }
    }

    @Before
    public void setUp() throws Exception {
        configureForUIThread();
        mNameVariableDialogFragment = new TestDialog(this);

        mActivity = mActivityRule.getActivity();
    }

    @Test
    public void dialogShowsOldVariableNameWhenRenaming() throws Exception {
        testTimeoutMs *= 5;  // Allow longer time for dialog interaction test

        String variableName = "oldVariableName";

        mNameVariableDialogFragment.setVariable(variableName,
                mock(NameVariableDialog.Callback.class), true);
        latch = new CountDownLatch(1);
        mNameVariableDialogFragment.show(mActivity.getSupportFragmentManager(), "RenameFragment");
        latch.await();
        assertEquals(LangUtils.interpolate("%{BKY_RENAME_VARIABLE_TITLE}").replace("%1", variableName), description);
    }

    @Test
    public void dialogShowsGenericTextForNewVariable() throws Exception {
        testTimeoutMs *= 5;  // Allow longer time for dialog interaction test

        mNameVariableDialogFragment.setVariable("", mock(NameVariableDialog.Callback.class), false);
        latch = new CountDownLatch(1);
        mNameVariableDialogFragment.show(mActivity.getSupportFragmentManager(), "CreateFragment");
        latch.await();
        assertEquals(LangUtils.interpolate("%{BKY_NEW_VARIABLE_TITLE}"), description);
    }
}
