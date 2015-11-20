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

package com.google.blockly.model;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A category of a toolbox, which holds zero or more blocks and zero or more subcategories.
 * {@link com.google.blockly.ui.ToolboxAdapter} is responsible for displaying this.
 */
public class ToolboxCategory {
    private final List<ToolboxCategory> mSubcategories = new ArrayList<>();
    private final List<Block> mBlocks = new ArrayList<>();
    // As displayed in the toolbox.
    private String mCategoryName;

    // For use in calculating positions in the toolbox.
    private boolean mIsExpanded = false;

    public String getCategoryName() {
        return mCategoryName;
    }

    public List<Block> getBlocks() {
        return mBlocks;
    }

    public List<ToolboxCategory> getSubcategories() {
        return mSubcategories;
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
     * @return True if this category should display all of its contents in the
     * {@link com.google.blockly.ui.ToolboxAdapter}; false if only its name should be shown.
     */
    public boolean isExpanded() {
        return mIsExpanded;
    }

    public void setExpanded(boolean expanded) {
        mIsExpanded = expanded;
    }

    public int getCurrentSize() {
        int size = 0;
        size += mSubcategories.size();
        size += mBlocks.size();
        for (int i = 0; i < mSubcategories.size(); i++) {
            if (mSubcategories.get(i).isExpanded()) {
                size += mSubcategories.get(i).getCurrentSize();
            }
        }
        return size;
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
        int eventType = parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagname = parser.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (parser.getName().equalsIgnoreCase("category")) {
                        result.addSubcategory(ToolboxCategory.fromXml(parser, factory));
                    } else if (parser.getName().equalsIgnoreCase("block")) {
                        result.addBlock(Block.fromXml(parser, factory));
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
