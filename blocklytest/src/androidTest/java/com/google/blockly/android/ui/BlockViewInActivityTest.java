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

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewParent;

import com.google.blockly.android.R;
import com.google.blockly.android.TestUtils;
import com.google.blockly.android.TestWorkspaceViewActivity;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * {@link BlockView} tests in the context of an attached Activity.
 */
public class BlockViewInActivityTest
        extends ActivityInstrumentationTestCase2<TestWorkspaceViewActivity> {
    private TestWorkspaceViewActivity mActivity;
    private Instrumentation mInstrumentation;
    private BlockFactory mBlockFactory;
    private WorkspaceHelper mHelper;
    private BlockViewFactory mViewFactory;

    // Block hierarchy, loaded from "controls_whileUntil".
    private Block mRootBlock;
    private Block mChildInputBlock;
    private Block mChildStatementBlock;
    private BlockView mRootView;
    private View mFieldView;
    private BlockView mChildInputBlockView;
    private BlockView mChildStatementBlockView;

    @Mock
    private ConnectionManager mMockConnectionManager;

    public BlockViewInActivityTest() {
        super(TestWorkspaceViewActivity.class);
    }

    @Override
    public void setUp() {
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();

        // To solve some issue with Dexmaker.  This allows us to use mockito.
        System.setProperty("dexmaker.dexcache", mActivity.getCacheDir().getPath());
        MockitoAnnotations.initMocks(this);

        mBlockFactory = new BlockFactory(mActivity.mThemeWrapper, new int[]{R.raw.test_blocks});
        mHelper = mActivity.mWorkspaceHelper;
        mViewFactory = mActivity.mViewFactory;
    }

    /**
     * Loads a {@code whileUntil} block instance with children into the workspace.
     */
    private void loadWhileUntilBlocksIntoWorkspaceView() {
        mRootBlock = mBlockFactory.obtainBlock("controls_whileUntil", "1");
        assertNotNull(mRootBlock);
        mChildInputBlock = mBlockFactory.obtainBlock("output_no_input", "2");
        mRootBlock.getInputByName("TIMES").getConnection()
                .connect(mChildInputBlock.getOutputConnection());
        mChildStatementBlock = mBlockFactory.obtainBlock("statement_no_input", "3");
        mRootBlock.getInputByName("NAME").getConnection()
                .connect(mChildStatementBlock.getPreviousConnection());

        mViewFactory.buildBlockGroupTree(mRootBlock, mMockConnectionManager, null);
        mRootView = mHelper.getView(mRootBlock);
        mFieldView = TestUtils.getFieldView(mRootView, mRootBlock.getFieldByName("MODE"));
        mChildInputBlockView = mHelper.getView(mChildInputBlock);
        mChildStatementBlockView = mHelper.getView(mChildStatementBlock);

        BlockGroup rootBlockGroup = (BlockGroup) mRootView.getParent();
        mActivity.mWorkspaceView.addView(rootBlockGroup);
    }

    /** Tests that pressed {@link View} state does not propagate to child BlockViews. */
    public void testActivatedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUntilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertTrue(isDescendentOf(mFieldView, (View) mViewFactory.getView(mRootBlock)));
        assertTrue(isDescendentOf((View) mChildInputBlockView, (View) mHelper.getView(mRootBlock)));
        assertTrue(isDescendentOf((View) mChildStatementBlockView,
                (View) mHelper.getView(mRootBlock)));

        assertFalse(((View) mRootView).isActivated());
        assertFalse(((View) mFieldView).isActivated());
        assertFalse(((View) mChildInputBlockView).isActivated());
        assertFalse(((View) mChildStatementBlockView).isActivated());

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((View) mRootView).setActivated(true);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be activated.
        assertTrue(((View) mRootView).isActivated());
        assertFalse(((View) mFieldView).isActivated());
        assertFalse(((View) mChildInputBlockView).isActivated());
        assertFalse(((View) mChildStatementBlockView).isActivated());
    }

    /** Tests that pressed {@link View} state does not propagate to child BlockViews. */
    public void testPressedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUntilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertTrue(isDescendentOf(mFieldView, (View) mHelper.getView(mRootBlock)));
        assertTrue(isDescendentOf((View) mChildInputBlockView, (View) mHelper.getView(mRootBlock)));
        assertTrue(isDescendentOf((View) mChildStatementBlockView,
                (View) mHelper.getView(mRootBlock)));

        assertFalse(((View) mRootView).isPressed());
        assertFalse(((View) mFieldView).isPressed());
        assertFalse(((View) mChildInputBlockView).isPressed());
        assertFalse(((View) mChildStatementBlockView).isPressed());

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((View) mRootView).setPressed(true);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be pressed.
        assertTrue(((View) mRootView).isPressed());
        assertFalse(((View) mFieldView).isPressed());
        assertFalse(((View) mChildInputBlockView).isPressed());
        assertFalse(((View) mChildStatementBlockView).isPressed());
    }

    /** Tests that focused {@link View} state does not propagate to child BlockViews. */
    public void testFocusedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUntilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertTrue(isDescendentOf(mFieldView, (View) mHelper.getView(mRootBlock)));
        assertTrue(isDescendentOf((View) mChildInputBlockView, (View) mHelper.getView(mRootBlock)));
        assertTrue(isDescendentOf((View) mChildStatementBlockView,
                (View) mHelper.getView(mRootBlock)));

        assertFalse(((View) mRootView).isFocused());
        assertFalse(mFieldView.isFocused());
        assertFalse(((View) mChildInputBlockView).isFocused());
        assertFalse(((View) mChildStatementBlockView).isFocused());

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((View) mRootView).requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be focused.
        assertTrue(((View) mRootView).isFocused());
        assertFalse(mFieldView.isFocused());
        assertFalse(((View) mChildInputBlockView).isFocused());
        assertFalse(((View) mChildStatementBlockView).isFocused());
    }

    /** Tests that selected {@link View} state does not propagate to child BlockViews. */
    public void testSelectedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUntilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertTrue(isDescendentOf(mFieldView, (View) mHelper.getView(mRootBlock)));
        assertTrue(isDescendentOf((View) mChildInputBlockView, (View) mHelper.getView(mRootBlock)));
        assertTrue(isDescendentOf((View) mChildStatementBlockView,
                (View) mHelper.getView(mRootBlock)));

        assertFalse(((View) mRootView).isSelected());
        assertFalse(mFieldView.isSelected());
        assertFalse(((View) mChildInputBlockView).isSelected());
        assertFalse(((View) mChildStatementBlockView).isSelected());

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((View) mRootView).setSelected(true);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be selected.
        assertTrue(((View) mRootView).isSelected());
        assertFalse(mFieldView.isSelected());
        assertFalse(((View) mChildInputBlockView).isSelected());
        assertFalse(((View) mChildStatementBlockView).isSelected());
    }

    private static boolean isDescendentOf(View child, View ancestor) {
        assertNotNull(child);
        assertNotNull(ancestor);

        ViewParent parent = child.getParent();
        while (parent != null && parent instanceof View) {
            if (ancestor == parent) {
                return true;
            }
            parent = ((View) parent).getParent();
        }
        return false;
    }
}
