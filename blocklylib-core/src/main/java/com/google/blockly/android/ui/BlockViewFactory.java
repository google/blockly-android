/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.android.ui;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.blockly.android.ToolboxFragment;
import com.google.blockly.android.TrashFragment;
import com.google.blockly.android.WorkspaceFragment;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.android.ui.fieldview.FieldView;
import com.google.blockly.model.Block;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base factory class responsible for creating all the
 * {@link com.google.blockly.android.ui.BlockView}s, {@link InputView}s, and {@link FieldView}s
 * for a given block representation.  All views are constructed during calls to
 * {@link #buildBlockViewTree}.
 */
public abstract class BlockViewFactory<BlockView extends com.google.blockly.android.ui.BlockView> {
    protected Context mContext;
    protected WorkspaceHelper mHelper;

    protected final Map<String,WeakReference<BlockView>> mBlockIdToView
            = Collections.synchronizedMap(new HashMap<String, WeakReference<BlockView>>());

    protected BlockViewFactory(Context context, WorkspaceHelper helper) {
        mContext = context;
        mHelper = helper;

        helper.setBlockViewFactory(this);
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mHelper;
    }

    public abstract void setVariableNameManager(NameManager variableNameManager);

    /**
     * Creates a {@link BlockGroup} for the given block and its children using the workspace's
     * default style.
     *
     * @param rootBlock The root block to generate a view for.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The {@link BlockTouchHandler} to manage all touches.
     *
     * @return A view for the block.
     */
    public BlockGroup buildBlockGroupTree(Block rootBlock,
                                          ConnectionManager connectionManager,
                                          BlockTouchHandler touchHandler) {
        BlockGroup bg = new BlockGroup(mContext, mHelper);
        buildBlockViewTree(rootBlock, bg, connectionManager, touchHandler);
        return bg;
    }

    /**
     * Called to construct the complete heirarchy of views representing a {@link Block} and it
     * subcomponents.  All constructed block views must be registered into the
     * {@link #mBlockIdToView} map, for later lookup via {@link #getView(Block)}.
     *
     * @param rootBlock The root block to generate a view for.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The {@link BlockTouchHandler} to manage all touches.
     *
     * @return A view for the block.
     */
    // TODO(#135): Implement tree traversal in this method and provide a build method for each view
    //             type.
    public abstract BlockView buildBlockViewTree(Block rootBlock, BlockGroup parentGroup,
                                                 ConnectionManager connectionManager,
                                                 BlockTouchHandler touchHandler);

    /**
     * This returns the view constructed to represent {@link Block}.  Each block is only allowed
     * one view instance among the view managed by this factory (including
     * {@link WorkspaceFragment}, {@link ToolboxFragment}, and {@link TrashFragment}. Views are
     * constructed in {@link #buildBlockViewTree}, either directly or via recursion.  If the block
     * view has not been constructed, this method will return null.
     * <p/>
     * Views are released by calling one of {@link BlockGroup#unlinkModelAndSubViews()},
     * {@link BlockView#unlinkModelAndSubViews()}, {@link InputView#unlinkModelAndSubViews()}, or
     * {@link FieldView#unlinkModel()}.
     *
     * @param block The Block to view.
     * @return The previously constructed and active view of {@code block}. Otherwise null.
     */
    @Nullable
    public BlockView getView(Block block) {
        WeakReference<BlockView> viewRef = mBlockIdToView.get(block.getId());
        return viewRef == null ? null : viewRef.get();
    }

    protected void registerView(Block block, BlockView blockView) {
        mBlockIdToView.put(block.getId(), new WeakReference<BlockView>(blockView));
    }
}
