/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.control;

import android.test.AndroidTestCase;

import com.google.blockly.R;
import com.google.blockly.TestUtils;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

/**
 * Tests for the {@link Dragger}.
 */
public class DraggerTest extends AndroidTestCase {
    @Mock
    ConnectionManager mConnectionManager;
    private WorkspaceHelper mWorkspaceHelper;
    private WorkspaceView mWorkspaceView;
    private Dragger mDragger;
    private BlockFactory mBlockFactory;
    private ArrayList<Block> mBlocks;

    @Override
    public void setUp() {
        // To solve some issue with Dexmaker.  This allows us to use mockito.
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        MockitoAnnotations.initMocks(this);

        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.toolbox_blocks});

        mBlocks = new ArrayList<>();
        mWorkspaceView = new WorkspaceView(getContext());
        mWorkspaceHelper = new WorkspaceHelper(mWorkspaceView, null);
        mDragger = new Dragger(mWorkspaceHelper, mWorkspaceView, mConnectionManager, mBlocks);
    }

    // Test connect statement
    public void testConnectToStatement() {
        // setup
        Block first = mBlockFactory.obtainBlock("statement_statement_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_statement_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_statement_input", "third block");
        Block fourth = mBlockFactory.obtainBlock("statement_no_next", "fourth block");
        mBlocks.add(first);
        mBlocks.add(second);
        mBlocks.add(third);
        mBlocks.add(fourth);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // No bump, no splice
        mDragger.removeFromRoot(second);
        mDragger.connectToStatement(first.getInputByName("statement input").getConnection(),
                second);

        assertSame(first, second.getPreviousBlock());
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));

        // Splice, no bump
        mDragger.removeFromRoot(third);
        mDragger.connectToStatement(first.getInputByName("statement input").getConnection(),
                third);

        assertSame(first, third.getPreviousBlock());
        assertSame(third, second.getPreviousBlock());

        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertSame(mWorkspaceHelper.getNearestParentBlockGroup(third),
                mWorkspaceHelper.getNearestParentBlockGroup(second));

        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));

        // Bump, no splice
        mDragger.removeFromRoot(fourth);
        mDragger.connectToStatement(first.getInputByName("statement input").getConnection(),
                fourth);

        assertSame(first, fourth.getPreviousBlock());
        // Third has been returned to the workspace root.
        assertNull(third.getPreviousBlock());
        assertTrue(mBlocks.contains(third));
        // First and fourth are connected.
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(fourth));
        // Second and third are separate.
        assertNotSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));
        // At workspace root.
        assertSame(mWorkspaceHelper.getRootBlockGroup(third),
                mWorkspaceHelper.getRootBlockGroup(second));
    }
}
