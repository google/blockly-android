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
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import com.google.blockly.android.TestUtils;
import com.google.blockly.android.TestWorkspaceViewActivity;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.utils.BlockLoadingException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static com.google.blockly.utils.ViewSubject.view;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link BlockView} tests in the context of an attached Activity.
 */
public class BlockViewInActivityTest {
    private TestWorkspaceViewActivity mActivity;
    private Instrumentation mInstrumentation;
    private BlocklyController mController;
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

    private ConnectionManager mMockConnectionManager;

    @Rule
    public ActivityTestRule<TestWorkspaceViewActivity> mActivityRule =
        new ActivityTestRule<>(TestWorkspaceViewActivity.class);

    @Before
    public void setUp() {
        mMockConnectionManager = mock(ConnectionManager.class);
        mActivity = mActivityRule.getActivity();
        mInstrumentation = InstrumentationRegistry.getInstrumentation();

        // To solve some issue with Dexmaker.  This allows us to use mockito.
        System.setProperty("dexmaker.dexcache", mActivity.getCacheDir().getPath());
        MockitoAnnotations.initMocks(this);


        mController = new BlocklyController.Builder(mActivity.mThemeWrapper)
                .addBlockDefinitionsFromAsset("default/test_blocks.json")
                .build();
        mBlockFactory = mController.getBlockFactory();
        mHelper = mActivity.mWorkspaceHelper;
        mViewFactory = mActivity.mViewFactory;
    }

    /**
     * Loads a {@code whileUntil} block instance with children into the workspace.
     */
    private void loadWhileUntilBlocksIntoWorkspaceView() {
        try {
            mRootBlock = mBlockFactory.obtainBlockFrom(
                    new BlockTemplate().ofType("controls_whileUntil"));
            assertThat(mRootBlock).isNotNull();
            mChildInputBlock = mBlockFactory.obtainBlockFrom(
                    new BlockTemplate().ofType("output_no_input"));
            mRootBlock.getInputByName("BOOL").getConnection()
                    .connect(mChildInputBlock.getOutputConnection());
            mChildStatementBlock = mBlockFactory.obtainBlockFrom(
                    new BlockTemplate().ofType("statement_no_input"));
            mRootBlock.getInputByName("DO").getConnection()
                    .connect(mChildStatementBlock.getPreviousConnection());

            mViewFactory.buildBlockGroupTree(mRootBlock, mMockConnectionManager, null);
            mRootView = mHelper.getView(mRootBlock);
            mFieldView = TestUtils.getFieldView(mRootView, mRootBlock.getFieldByName("MODE"));
            mChildInputBlockView = mHelper.getView(mChildInputBlock);
            mChildStatementBlockView = mHelper.getView(mChildStatementBlock);

            BlockGroup rootBlockGroup = (BlockGroup) mRootView.getParent();
            mActivity.mWorkspaceView.addView(rootBlockGroup);
        } catch (BlockLoadingException e) {
            throw new RuntimeException(e);
        }
    }

    /** Tests that pressed {@link View} state does not propagate to child BlockViews. */
    @Test
    public void testActivatedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUntilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertAbout(view()).that(mFieldView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));
        assertAbout(view()).that((View) mChildInputBlockView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));
        assertAbout(view()).that((View) mChildStatementBlockView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));

        assertThat(((View) mRootView).isActivated()).isFalse();
        assertThat(((View) mFieldView).isActivated()).isFalse();
        assertThat(((View) mChildInputBlockView).isActivated()).isFalse();
        assertThat(((View) mChildStatementBlockView).isActivated()).isFalse();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((View) mRootView).setActivated(true);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be activated.
        assertThat(((View) mRootView).isActivated()).isTrue();
        assertThat(((View) mFieldView).isActivated()).isFalse();
        assertThat(((View) mChildInputBlockView).isActivated()).isFalse();
        assertThat(((View) mChildStatementBlockView).isActivated()).isFalse();
    }

    /** Tests that pressed {@link View} state does not propagate to child BlockViews. */
    @Test
    public void testPressedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUntilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertAbout(view()).that(mFieldView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));
        assertAbout(view()).that((View) mChildInputBlockView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));
        assertAbout(view()).that((View) mChildStatementBlockView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));

        assertThat(((View) mRootView).isPressed()).isFalse();
        assertThat(((View) mFieldView).isPressed()).isFalse();
        assertThat(((View) mChildInputBlockView).isPressed()).isFalse();
        assertThat(((View) mChildStatementBlockView).isPressed()).isFalse();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((View) mRootView).setPressed(true);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be pressed.
        assertThat(((View) mRootView).isPressed()).isTrue();
        assertThat(((View) mFieldView).isPressed()).isFalse();
        assertThat(((View) mChildInputBlockView).isPressed()).isFalse();
        assertThat(((View) mChildStatementBlockView).isPressed()).isFalse();
    }

    /** Tests that focused {@link View} state does not propagate to child BlockViews. */
    @Test
    public void testFocusedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUntilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertAbout(view()).that(mFieldView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));
        assertAbout(view()).that((View) mChildInputBlockView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));
        assertAbout(view()).that((View) mChildStatementBlockView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));

        assertThat(((View) mRootView).isFocused()).isFalse();
        assertThat(mFieldView.isFocused()).isFalse();
        assertThat(((View) mChildInputBlockView).isFocused()).isFalse();
        assertThat(((View) mChildStatementBlockView).isFocused()).isFalse();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((View) mRootView).requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be focused.
        assertThat(((View) mRootView).isFocused()).isTrue();
        assertThat(mFieldView.isFocused()).isFalse();
        assertThat(((View) mChildInputBlockView).isFocused()).isFalse();
        assertThat(((View) mChildStatementBlockView).isFocused()).isFalse();
    }

    /** Tests that selected {@link View} state does not propagate to child BlockViews. */
    @Test
    public void testSelectedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUntilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertAbout(view()).that(mFieldView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));
        assertAbout(view()).that((View) mChildInputBlockView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));
        assertAbout(view()).that((View) mChildStatementBlockView)
                .isDescendantOf(mViewFactory.getView(mRootBlock));

        assertThat(((View) mRootView).isSelected()).isFalse();
        assertThat(mFieldView.isSelected()).isFalse();
        assertThat(((View) mChildInputBlockView).isSelected()).isFalse();
        assertThat(((View) mChildStatementBlockView).isSelected()).isFalse();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((View) mRootView).setSelected(true);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be selected.
        assertThat(((View) mRootView).isSelected()).isTrue();
        assertThat(mFieldView.isSelected()).isFalse();
        assertThat(((View) mChildInputBlockView).isSelected()).isFalse();
        assertThat(((View) mChildStatementBlockView).isSelected()).isFalse();
    }
}
