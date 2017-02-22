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
import android.support.test.InstrumentationRegistry;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.test.R;
import com.google.blockly.android.ui.fieldview.FieldView;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.Input;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractInputView}.
 */
public class AbstractInputViewTest extends BlocklyTestCase {

    private Input mDummyInput;
    private WorkspaceHelper mMockWorkspaceHelper;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mMockWorkspaceHelper = mock(WorkspaceHelper.class);
        // Use the BlockFactory to make sure we have real inputs.
        // TODO(#435): Replace R.raw.test_blocks
        BlockFactory factory = new BlockFactory(InstrumentationRegistry.getContext(),
                new int[]{R.raw.test_blocks});
        Block block = factory.obtainBlock("test_block_one_input_each_type", "fake_id");
        mDummyInput = block.getInputs().get(0);
        assertThat(mDummyInput.getType()).isEqualTo(Input.TYPE_DUMMY);
    }

    // Verify correct object state after construction.
    @Test
    public void testConstructor() {
        final AbstractInputView inputView = makeDefaultInputView();

        // Verify Input and InputView are linked both ways.
        assertThat(mDummyInput).isSameAs(inputView.getInput());
        assertThat(inputView).isSameAs(mDummyInput.getView());
    }

    // Verify child view can be set.
    @Test
    public void testSetChildView() {
        final AbstractInputView inputView = makeDefaultInputView();
        assertThat(inputView.getChildCount()).isEqualTo(0);

        final BlockGroup mockGroup = mock(BlockGroup.class);
        inputView.setConnectedBlockGroup(mockGroup);
        assertThat(mockGroup).isSameAs(inputView.getConnectedBlockGroup());
        assertThat(inputView.getChildCount()).isEqualTo(1);
    }

    // Verify child view can be set, unset, then set again.
    @Test
    public void testUnsetChildView() {
        final AbstractInputView inputView = makeDefaultInputView();

        final BlockGroup mockGroup = mock(BlockGroup.class);
        inputView.setConnectedBlockGroup(mockGroup);
        inputView.setConnectedBlockGroup(null);
        assertThat(inputView.getConnectedBlockGroup()).isNull();
        assertThat(inputView.getChildCount()).isEqualTo(0);

        inputView.setConnectedBlockGroup(mockGroup);
        assertThat(mockGroup).isSameAs(inputView.getConnectedBlockGroup());
        assertThat(inputView.getChildCount()).isEqualTo(1);
    }

    // Verify exception is thrown when calling setChildView repeatedly without disconnectBlockGroup.
    @Test
    public void testSetChildViewMustUnset() {
        final AbstractInputView inputView = makeDefaultInputView();

        final BlockGroup mockView = mock(BlockGroup.class);
        inputView.setConnectedBlockGroup(mockView);

        thrown.expect(IllegalStateException.class);
        inputView.setConnectedBlockGroup(mockView);
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
