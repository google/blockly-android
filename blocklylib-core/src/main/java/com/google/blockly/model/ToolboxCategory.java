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

package com.google.blockly.model;

import android.text.TextUtils;
import android.util.Log;

import com.google.blockly.android.ToolboxFragment;
import com.google.blockly.utils.ColorUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * A category of a toolbox, which holds zero or more blocks and zero or more subcategories.
 * {@link ToolboxFragment} is responsible for displaying this.
 */
public class ToolboxCategory {
    private static final String TAG = "ToolboxCategory";

    /** Array used for by {@link ColorUtils#parseColor(String, float[], int)} during I/O. **/
    private static final float[] TEMP_IO_THREAD_FLOAT_ARRAY = new float[3];

    private final List<ToolboxCategory> mSubcategories = new ArrayList<>();
    private final List<Block> mBlocks = new ArrayList<>();
    // As displayed in the toolbox.
    private String mCategoryName;
    private String mCustomType;
    private Integer mColor = null;

    public String getCategoryName() {
        return mCategoryName;
    }

    public String getCustomType() {
        return mCustomType;
    }

    public List<Block> getBlocks() {
        return mBlocks;
    }

    public List<ToolboxCategory> getSubcategories() {
        return mSubcategories;
    }

    public Integer getColor() {
        return mColor;
    }

    /**
     * Add a {@link Block} to the blocks displayed in this category.
     *
     * @param block The {@link Block} to add.
     */
    public void addBlock(Block block) {
        mBlocks.add(block);
    }

    /**
     * Clear the contents of this category and all subcategories; remove subcategories.
     */
    public void clear() {
        for (int i = 0; i < mSubcategories.size(); i++) {
            mSubcategories.get(i).clear();
        }
        mBlocks.clear();
        mSubcategories.clear();
    }

    public boolean isEmpty() {
        return mSubcategories.isEmpty() && mBlocks.isEmpty();
    }

    /**
     * Fill the given list with of the {@link Block} instances in this category and its
     * subcategories.
     *
     * @param blocks The list to add to, which is not cleared before adding blocks.
     */
    public void getAllBlocksRecursive(List<Block> blocks) {
        blocks.addAll(mBlocks);
        for (int i = 0; i < mSubcategories.size(); i++) {
            mSubcategories.get(i).getAllBlocksRecursive(blocks);
        }
    }

    private void addSubcategory(ToolboxCategory subcategory) {
        mSubcategories.add(subcategory);
    }

    /**
     * Read the full definition of the category's contents in from XML.
     *
     * @param parser The {@link XmlPullParser} to read from.
     * @param factory The {@link BlockFactory} to use to generate blocks from their names.
     *
     * @return A new {@link ToolboxCategory} with the contents given by the XML.
     * @throws IOException when reading from the parser fails.
     * @throws XmlPullParserException when reading from the parser fails.
     */
    public static ToolboxCategory fromXml(XmlPullParser parser, BlockFactory factory)
            throws IOException, XmlPullParserException {
        ToolboxCategory result = new ToolboxCategory();
        result.mCategoryName = parser.getAttributeValue("", "name");
        result.mCustomType = parser.getAttributeValue("", "custom");
        String colourAttr = parser.getAttributeValue("", "colour");
        if (!TextUtils.isEmpty(colourAttr)) {
            try {
                result.mColor = ColorUtils.parseColor(colourAttr, TEMP_IO_THREAD_FLOAT_ARRAY);
            } catch (ParseException e) {
                Log.w(TAG, "Invalid toolbox category colour \"" + colourAttr + "\"");
            }
        }
        int eventType = parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagname = parser.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (parser.getName().equalsIgnoreCase("category")) {
                        result.addSubcategory(ToolboxCategory.fromXml(parser, factory));
                    } else if (parser.getName().equalsIgnoreCase("block")) {
                        result.addBlock(Block.fromXml(parser, factory));
                    } else if (parser.getName().equalsIgnoreCase("shadow")) {
                        throw new IllegalArgumentException(
                                "Shadow blocks may not be top level toolbox blocks.");
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (tagname.equalsIgnoreCase("category")) {
                        return result;
                    }
                    break;
                default:
                    break;
            }
            eventType = parser.next();
        }
        return result;
    }
}
