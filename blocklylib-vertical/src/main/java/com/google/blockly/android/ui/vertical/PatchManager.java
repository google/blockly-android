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

package com.google.blockly.android.ui.vertical;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.model.Block;

/**
 * Helper class to load and manage 9-patches and other drawables. Each block shape is composed of
 * multiple shaded 9-patches that together make the overall shape. The PatchManager constructs new
 * drawables with shared backing bitmaps to minimize Blocky's memory footprint.
 */
public class PatchManager {
    private final Context mContext;
    private final Resources mResources;

    private final Rect mTempRect = new Rect();

    // Horizontal block padding - this space accomodates left and right block boundaries.
    int mBlockStartPadding;
    int mBlockEndPadding;

    // Vertical block padding - this space accomodates top and bottom block boundaries.
    int mBlockTopDefaultPadding;
    int mBlockTopHatPadding;
    int mBlockTopOutputPadding;
    int mBlockTopPreviousPadding;
    int mBlockTopDefaultMinPadding;
    int mBlockTopHatMinPadding;
    int mBlockBottomPadding;

    // Convenience fields - these are the sums of mBlockStartPadding and mBlockEndPadding, or
    // mBlockTopPadding and mBlockBottomPadding, respectively.
    int mBlockTotalPaddingX;

    // Height of the "Next" connector - this is in addition to the bottom block boundary thickness,
    // which is mBlockBottomPadding.
    int mNextConnectorHeight;

    // Width of the "Output" connector - this is in addition to the left (right, in RTL mode) block
    // boundary thickness, which is mBlockStartPadding.
    int mOutputConnectorWidth;

    // Intrinsic height of the "Output" connector patch.
    int mOutputConnectorHeight;

    // Width of a value input. This is in addition to the width of the right block boundary (left
    // boundary in RTL mode) in a block without inputs (the latter is mBlockEndPadding).
    int mValueInputWidth;

    // Minimum indent of the Statement connector w.r.t. the right side of the block (left side in
    // RTL mode).
    int mStatementInputIndent;

    // Padding between rightmost (leftmost, in RTL mode) field of the Statement input and the
    // placement of a connected block.
    int mStatementInputPadding;

    // Thickness of the Statement connector top - this is the vertical offset of a connected block
    // relative to the top of the Statement input layout area.
    int mStatementTopThickness;

    // Thickness of the Statement connector bottom - this is the vertical offset between the bottom
    // of a connected block and the bottom of the Statement input layout area.
    int mStatementBottomThickness;

    // Minimum height of a Statement connector - this accomodates the added intrinsic heights of
    // connector top and bottom patches.
    int mStatementMinHeight;

    // Minimum width of an (empty) inline input connector.
    int mInlineInputMinimumWidth;

    // Minimum height of an (empty) inline input connector.
    int mInlineInputMinimumHeight;

    // Start padding of the inline input connector - this is the horizontal offset between the
    // connector position and the position of a connected block.
    int mInlineInputStartPadding;

    // Top padding of the inline input connector - this is the vertical offset between the connector
    // top and the top of a connected block.
    int mInlineInputTopPadding;

    // Total horizontal padding of the inline connector - the total width occupied by a connector is
    // this value plus the width of connected blocks.
    int mInlineInputTotalPaddingX;

    // Total vertical padding of the inline connector - the total height occupied by a connector is
    // this value plus the height of connected blocks.
    int mInlineInputTotalPaddingY;

    // Minimum height of a block.
    int mMinBlockHeight;

    /**
     * Constructs a new PatchManager to load and manage 9-patch resources used to compose the block
     * shapes.
     * @param context The activity's context.
     * @param rtl Whether to use right-to-left resources.
     * @param useHat Whether to use event hats when blocks have neither previous or output
     *               connectors.  See {@link R.drawable#top_start_hat} nine-patch.
     */
    PatchManager(Context context, boolean rtl, boolean useHat) {
        mContext = context;
        mResources = mContext.getResources();
        computePatchLayoutMeasures(rtl, useHat);
    }

    /**
     * Get a patch drawable.
     *
     * @param id The resource Id of the patch.
     * @return The drawable for the requested patch.
     */
    public NinePatchDrawable getPatchDrawable(int id) {
        return (NinePatchDrawable) ContextCompat.getDrawable(mContext, id);
    }

    /**
     * Compute layout measures such as offsets and paddings from the various block patches.
     */
    private void computePatchLayoutMeasures(boolean rtl, boolean useHat) {
        final Configuration config = mResources.getConfiguration();

        if (!getPatchDrawable(R.drawable.top_start_default).getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'top_start_default' does not have padding.");
        }
        mBlockStartPadding = rtl ? mTempRect.right : mTempRect.left;

        mBlockEndPadding = getPatchDrawable(R.drawable.dummy_input).getIntrinsicWidth();
        mValueInputWidth = getPatchDrawable(R.drawable.value_input_external).getIntrinsicWidth() -
                mBlockEndPadding;

        final NinePatchDrawable bottomPatchDefault =
                getPatchDrawable(R.drawable.bottom_start_default);
        if (!bottomPatchDefault.getPadding(mTempRect)) {
            throw new IllegalStateException(
                    "9-patch 'bottom_start_default' does not have padding.");
        }
        mBlockBottomPadding = mTempRect.bottom;

        final NinePatchDrawable bottomPatchNext = getPatchDrawable(R.drawable.bottom_start_next);
        if (!bottomPatchNext.getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'bottom_start_next' does not have padding.");
        }
        mNextConnectorHeight = mTempRect.bottom - mBlockBottomPadding;

        final NinePatchDrawable topLeftDefaultPatch = getPatchDrawable(R.drawable.top_start_default);
        if (!topLeftDefaultPatch.getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'top_start_default' does not have padding.");
        };
        mBlockTopDefaultPadding = mTempRect.top;

        final NinePatchDrawable topLeftHatPatch = getPatchDrawable(R.drawable.top_start_hat);
        if (!topLeftHatPatch.getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'top_start_hat' does not have padding.");
        };
        mBlockTopHatPadding = mTempRect.top;

        final NinePatchDrawable topLeftPreviousPatch =
                getPatchDrawable(R.drawable.top_start_previous);
        if (!topLeftPreviousPatch.getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'top_start_previous' does not have padding.");
        };
        mBlockTopPreviousPadding = mTempRect.top;

        final NinePatchDrawable topLeftOutputPatch = getPatchDrawable(R.drawable.top_start_output);
        if (!topLeftOutputPatch.getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'top_start_output' does not have padding.");
        };
        mBlockTopOutputPadding = mTempRect.top;
        mOutputConnectorWidth = (rtl ? mTempRect.right : mTempRect.left) - mBlockStartPadding;
        mOutputConnectorHeight = topLeftOutputPatch.getIntrinsicHeight();

        mBlockTopDefaultMinPadding = Math.min(mBlockTopDefaultPadding,
                Math.min(mBlockTopOutputPadding, mBlockTopPreviousPadding));
        mBlockTopHatMinPadding = Math.min(mBlockTopHatPadding,
                Math.min(mBlockTopOutputPadding, mBlockTopPreviousPadding));

        // Block height must be sufficient to at least accommodate vertical padding and an Output
        // connector.
        mMinBlockHeight = mBlockTopDefaultMinPadding + mOutputConnectorHeight + mBlockBottomPadding;

        final NinePatchDrawable statementTopPatch = getPatchDrawable(R.drawable.statementinput_top);
        if (!statementTopPatch.getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'statementinput_top' does not have padding.");
        };
        mStatementTopThickness = mTempRect.top;
        mStatementInputIndent = statementTopPatch.getIntrinsicWidth();
        mStatementInputPadding = rtl ? mTempRect.right : mTempRect.left;

        final NinePatchDrawable statementBottomPatch =
                getPatchDrawable(R.drawable.statementinput_bottom);
        if (!statementBottomPatch.getPadding(mTempRect)) {
            throw new IllegalStateException(
                    "9-patch 'statementinput_bottom' does not have padding.");
        }
        mStatementBottomThickness = mTempRect.bottom;

        mStatementMinHeight =
                statementTopPatch.getIntrinsicHeight() + statementBottomPatch.getIntrinsicHeight();

        final NinePatchDrawable inlineInputPatch = getPatchDrawable(R.drawable.value_input_inline);
        mInlineInputMinimumWidth = inlineInputPatch.getIntrinsicWidth();
        mInlineInputMinimumHeight = inlineInputPatch.getIntrinsicHeight();

        inlineInputPatch.getPadding(mTempRect);
        mInlineInputStartPadding = rtl ? mTempRect.right : mTempRect.left;
        mInlineInputTopPadding = mTempRect.top;
        mInlineInputTotalPaddingX = mTempRect.left + mTempRect.right;
        mInlineInputTotalPaddingY = mTempRect.top + mTempRect.bottom;

        // Convenience fields.
        mBlockTotalPaddingX = mBlockStartPadding + mBlockEndPadding;
    }

    int computeBlockGroupTopPadding(@Nullable BlockGroup blockGroup) {
        if (blockGroup == null) {
            return mBlockTopDefaultPadding;
        }
        BlockView firstBlockView = (BlockView) blockGroup.getFirstBlockView();
        return firstBlockView == null ? mBlockTopDefaultPadding
                : computeBlockTopPadding(firstBlockView);
    }

    int computeBlockTopPadding(BlockView blockView) {
        Block block = blockView.getBlock();
        if (block.getPreviousConnection() != null) {
            return mBlockTopPreviousPadding;
        } else if (block.getOutputConnection() != null) {
            return mBlockTopOutputPadding;
        } else if (blockView.hasCap()) {
            return mBlockTopHatPadding;
        } else {
            return mBlockTopDefaultPadding;
        }
    }
}
