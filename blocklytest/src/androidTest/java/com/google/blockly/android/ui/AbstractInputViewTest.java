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

import android.support.annotation.NonNull;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.R;
import com.google.blockly.android.ui.fieldview.FieldView;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.Input;

import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;

/**
 * Tests for {@link AbstractInputView}.
 */
@SmallTest
public class AbstractInputViewTest extends MockitoAndroidTestCase {

    private Input mDummyInput;

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Use the BlockFactory to make sure we have real inputs.
        BlockFactory factory = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = factory.obtainBlock("test_block_one_input_each_type", "fake_id");
        mDummyInput = block.getInputs().get(0);
        assertEquals(Input.TYPE_DUMMY, mDummyInput.getType());
    }

    // Verify correct object state after construction.
    public void testConstructor() {
        final AbstractInputView inputView = makeDefaultInputView();

        // Verify Input and InputView are linked both ways.
        assertSame(mDummyInput, inputView.getInput());
        assertSame(inputView, mDummyInput.getView());
    }

    // Verify child view can be set.
    public void testSetChildView() {
        final AbstractInputView inputView = makeDefaultInputView();
        assertEquals(0, inputView.getChildCount());

        final BlockGroup mockGroup = Mockito.mock(BlockGroup.class);
        inputView.setConnectedBlockGroup(mockGroup);
        assertSame(mockGroup, inputView.getConnectedBlockGroup());
        assertEquals(1, inputView.getChildCount());
    }

    // Verify child view can be set, unset, then set again.
    public void testUnsetChildView() {
        final AbstractInputView inputView = makeDefaultInputView();

        final BlockGroup mockGroup = Mockito.mock(BlockGroup.class);
        inputView.setConnectedBlockGroup(mockGroup);
        inputView.setConnectedBlockGroup(null);
        assertNull(inputView.getConnectedBlockGroup());
        assertEquals(0, inputView.getChildCount());

        inputView.setConnectedBlockGroup(mockGroup);
        assertSame(mockGroup, inputView.getConnectedBlockGroup());
        assertEquals(1, inputView.getChildCount());
    }

    // Verify exception is thrown when calling setChildView repeatedly without disconnectBlockGroup.
    public void testSetChildViewMustUnset() {
        final AbstractInputView inputView = makeDefaultInputView();

        final BlockGroup mockView = Mockito.mock(BlockGroup.class);
        inputView.setConnectedBlockGroup(mockView);

        try {
            inputView.setConnectedBlockGroup(mockView);
        } catch (IllegalStateException expected) {
            return;
        }

        fail("Expected IllegalStateException not thrown.");
    }

    @NonNull
    private AbstractInputView makeDefaultInputView() {
        return new AbstractInputView(
                getContext(), mMockWorkspaceHelper, mDummyInput, new ArrayList<FieldView>()) {

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                // Fake. Do nothing.
            }
        };
    }
}
