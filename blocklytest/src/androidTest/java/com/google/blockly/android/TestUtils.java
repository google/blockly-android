/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.android;

import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.android.ui.BlockView;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.android.ui.fieldview.FieldView;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockDefinition;
import com.google.blockly.model.Field;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

import static com.google.blockly.model.BlockFactory.block;

/**
 * Utils for setting up blocks during testing.
 */
public final class TestUtils {

    private TestUtils() {
    }

    /**
     * Create views for the given blocks and add them to the workspace given by the combination
     * of connection manager, helper, and view.
     */
    public static void createViews(List<Block> blocks, BlockViewFactory viewFactory,
                                   ConnectionManager connectionManager, WorkspaceView workspaceView) {

        // Create views for all of the blocks we're interested in.
        for (int i = 0; i < blocks.size(); i++) {
            workspaceView.addView(
                    viewFactory.buildBlockGroupTree(blocks.get(i), connectionManager, null));
        }
    }

    /**
     * Traverses the view tree for the FieldView for {@code field} of block, while ignoring
     * subblocks and related subviews.
     *
     * @param blockView The view for {@code field}'s parent {@link Block}.
     * @param field     The field to find a {@link FieldView}.
     * @return The {@link View} / {@link FieldView} for {@code field}, or null if not found.
     */
    public static View getFieldView(BlockView blockView, Field field) {
        return getFieldViewImpl((ViewGroup) blockView, field);
    }

    // Recursive implementation of above.
    private static View getFieldViewImpl(ViewGroup parent, Field field) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View child = parent.getChildAt(i);
            if (child instanceof BlockView) {
                continue; // Skip embedded blocks
            }
            if (child instanceof FieldView) {
                if (((FieldView) child).getField() == field) {
                    return child;
                } else {
                    continue;
                }
            }
            if (child instanceof ViewGroup) {
                View childResult = getFieldViewImpl((ViewGroup) child, field);
                if (childResult != null) {
                    return childResult;
                }
            }
        }
        return null;  // Not found.
    }

    /**
     * Creates a {@link BlockDefinition} that is a stub for a procedure definition block.
     *
     * @param procedureName The name of the procedure this block defines.
     * @return The block definition
     */
    public static BlockDefinition getProcedureDefinitionBlockDefinition(String procedureName) {
        try {
            String json = getProcedureBlockJson(
                    ProcedureManager.PROCEDURE_DEFINITION_PREFIX + procedureName,
                    procedureName);
            return new BlockDefinition(json);
        } catch (JSONException e) {
            // Shouldn't ever happen
            throw new IllegalStateException("Failed to create and parse JSON block definition.");
        }
    }

    /**
     * Creates a {@link BlockDefinition} that is a stub for a procedure reference block.
     *
     * @param procedureName The name of the procedure this block references.
     * @return The block definition
     */
    public static BlockDefinition getProcedureReferenceBlockDefinition(String procedureName) {
        try {
            String json = getProcedureBlockJson(
                    ProcedureManager.PROCEDURE_REFERENCE_PREFIX + procedureName,
                    procedureName);
            return new BlockDefinition(json);
        } catch (JSONException e) {
            // Shouldn't ever happen
            throw new IllegalStateException("Failed to create and parse JSON block definition.");
        }
    }

    /**
     * Creates the JSON that defines or references a procedure (depending on the prefix in
     * {@code blockType}), with one field named "name" with a value of {@code procedureName}.
     *
     * @param blockType The type name of the block, which should be the procedure name with a prefix
     * @param procedureName The name of the procedure this block defines/references.
     * @return A JSON string block definition.
     * @throws JSONException If JSONStringer cannot quote either parameter.
     */
    private static String getProcedureBlockJson(String blockType, String procedureName)
            throws JSONException
    {
        String typeQuotedAndEscaped = JSONObject.quote(blockType);
        String nameQuotedAndEscaped = JSONObject.quote(procedureName);
        String prefix = "{\"type\":";  // Quoted block type goes here
        String middle = ", \"message0\":\"%1\"," +
                        "\"args0\":[{" +
                            "\"type\":\"field_input\"," +
                            "\"name\":\"name\"," +
                            "\"text\":"; // Quoted procedure name goes here
        String suffix = "}]}";
        return prefix + typeQuotedAndEscaped + middle + nameQuotedAndEscaped + suffix;
    }
}
