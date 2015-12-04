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

package com.google.blockly.ui;

import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.util.Log;

import com.google.blockly.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to manage patches, including 9-patches, for drawing blocks.
 */
public class PatchManager {
    private final Resources mResources;

    private final Rect mTempRect = new Rect();
    private final Map<Integer, Integer> mRightToLeftPatchTable = new HashMap<>();

    // Horizontal block padding - this space accomodates left and right block boundaries.
    int mBlockLeftPadding;
    int mBlockRightPadding;

    // Vertical block padding - this space accomodates top and bottom block boundaries.
    int mBlockTopPadding;
    int mBlockBottomPadding;

    // Convenience fields - these are the sums of mBlockLeftPadding and mBlockRightPadding, or
    // mBlockTopPadding and mBlockBottomPadding, respectively.
    int mBlockTotalPaddingX;
    int mBlockTotalPaddingY;

    // Height of the "Next" connector - this is in addition to the bottom block boundary thickness,
    // which is mBlockBottomPadding.
    int mNextConnectorHeight;

    // Width of the "Output" connector - this is in addition to the left block boundary thickness,
    // which is mBlockLeftPadding.
    int mOutputConnectorWidth;

    // Intrinsic height of the "Output" connector patch.
    int mOutputConnectorHeight;

    // Width of a value input. This is in addition to the width of the right block boundary in a
    // block without inputs (the latter is mBlockRightPadding).
    int mValueInputWidth;

    // Minimum indent of the Statement connector w.r.t. the right side of the block.
    int mStatementInputIndent;

    // Padding between rightmost field of the Statement input and the placement of a connected
    // block.
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

    // Left padding of the inline input connector - this is the horizontal offset between the
    // connector position and the position of a connected block.
    int mInlineInputLeftPadding;

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

    PatchManager(Resources resources) {
        mResources = resources;

        computePatchLayoutMeasures();
        makeRightToLeftPatchTable();
    }

    /** Build lookup table from Left-to-Right 9-patch resource Ids to Right-to-Left Ids. */
    private void makeRightToLeftPatchTable() {
        // Bottom-left corner (in LtR mode; bottom-right in RtL).
        mRightToLeftPatchTable.put(R.drawable.bl_default, R.drawable.bl_default_rtl);
        mRightToLeftPatchTable.put(R.drawable.bl_next, R.drawable.bl_next_rtl);

        // Top-left corner (in LtR mode; top-right in RtL).
        mRightToLeftPatchTable.put(R.drawable.tl_default, R.drawable.tl_default_rtl);
        mRightToLeftPatchTable.put(R.drawable.tl_output, R.drawable.tl_output_rtl);
        mRightToLeftPatchTable.put(R.drawable.tl_prev, R.drawable.tl_prev_rtl);

        // Value inputs.
        mRightToLeftPatchTable.put(
                R.drawable.value_input_external, R.drawable.value_input_external_rtl);
        mRightToLeftPatchTable.put(
                R.drawable.value_input_inline, R.drawable.value_input_inline_rtl);

        // Dummy input.
        mRightToLeftPatchTable.put(R.drawable.dummy_input, R.drawable.dummy_input_rtl);

        // Statement inputs.
        mRightToLeftPatchTable.put(
                R.drawable.statementinput_top, R.drawable.statementinput_top_rtl);
        mRightToLeftPatchTable.put(
                R.drawable.statementinput_bottom, R.drawable.statementinput_bottom_rtl);
    }

    /**
     * Get a patch drawable.
     *
     * @param id The resource Id of the patch.
     * @return The drawable for the requested patch.
     */
    public NinePatchDrawable getPatchDrawable(int id) {
        return getPatchDrawable(id, false);
    }

    /**
     * Get a patch drawable for either left-to-right or right-to-left layout.
     *
     * @param id The resource Id of the patch.
     * @param rtl If this is true, the patch for right-to-left layouts is produced. Otherwise, the
     * patch for left-to-right layouts is returned.
     * @return The drawable for the requested patch.
     */
    public NinePatchDrawable getPatchDrawable(int id, boolean rtl) {
        if (rtl) {
            return (NinePatchDrawable) mResources.getDrawable(mRightToLeftPatchTable.get(id));
        } else {
            return (NinePatchDrawable) mResources.getDrawable(id);
        }
    }

    /**
     * Compute layout measures such as offsets and paddings from the various block patches.
     */
    private void computePatchLayoutMeasures() {
        if (!getPatchDrawable(R.drawable.tl_default).getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'tl_default' does not have padding.");
        }
        mBlockLeftPadding = mTempRect.left;

        mBlockRightPadding = getPatchDrawable(R.drawable.dummy_input).getIntrinsicWidth();
        mValueInputWidth = getPatchDrawable(R.drawable.value_input_external).getIntrinsicWidth() -
                mBlockRightPadding;

        final NinePatchDrawable bottomPatchDefault = getPatchDrawable(R.drawable.bl_default);
        if (!bottomPatchDefault.getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'bl_default' does not have padding.");
        }
        mBlockBottomPadding = mTempRect.bottom;

        // Convenience fields.
        mBlockTotalPaddingX = mBlockLeftPadding + mBlockRightPadding;
        mBlockTotalPaddingY = mBlockTopPadding + mBlockBottomPadding;

        final NinePatchDrawable bottomPatchNext = getPatchDrawable(R.drawable.bl_next);
        if (!bottomPatchNext.getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'bl_next' does not have padding.");
        }
        mNextConnectorHeight = mTempRect.bottom - mBlockBottomPadding;

        final NinePatchDrawable topLeftOutputPatch = getPatchDrawable(R.drawable.tl_output);
        if (!topLeftOutputPatch.getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'tl_output' does not have padding.");
        };
        mBlockTopPadding = mTempRect.top;
        mOutputConnectorWidth = mTempRect.left - mBlockLeftPadding;
        mOutputConnectorHeight = topLeftOutputPatch.getIntrinsicHeight();

        // Block height must be sufficient to at least accomodate vertical padding and an Output
        // connector.
        mMinBlockHeight = mBlockTotalPaddingY + mOutputConnectorHeight;

        final NinePatchDrawable statementTopPatch = getPatchDrawable(R.drawable.statementinput_top);
        if (!statementTopPatch.getPadding(mTempRect)) {
            throw new IllegalStateException("9-patch 'statementinput_top' does not have padding.");
        };
        mStatementTopThickness = mTempRect.top;
        mStatementInputIndent = statementTopPatch.getIntrinsicWidth();
        mStatementInputPadding = mTempRect.left;

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
        mInlineInputLeftPadding = mTempRect.left;
        mInlineInputTopPadding = mTempRect.top;
        mInlineInputTotalPaddingX = mTempRect.left + mTempRect.right;
        mInlineInputTotalPaddingY = mTempRect.top + mTempRect.bottom;
    }
}
