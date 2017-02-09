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

import com.google.blockly.android.FlyoutFragment;
import com.google.blockly.utils.ColorUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * A category of a toolbox, which holds zero or more blocks or zero or more subcategories. Not both.
 * {@link FlyoutFragment} is responsible for displaying this.
 */
public class FlyoutCategory {
    private static final String TAG = "FlyoutCategory";

    /** Array used for by {@link ColorUtils#parseColor(String, float[], int)} during I/O. **/
    private static final float[] TEMP_IO_THREAD_FLOAT_ARRAY = new float[3];

    private final List<FlyoutCategory> mSubcategories = new ArrayList<>();
    private final List<Block> mBlocks = new ArrayList<>();
    // As displayed in the toolbox.
    private String mCategoryName;
    private String mCustomType;
    private Integer mColor = null;
    private boolean mIsVariableCategory = false;
    private boolean mIsFunctionCategory = false;
    private Callback mCallback;

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    /**
     * @return The user visible name of this category.
     */
    public String getCategoryName() {
        return mCategoryName;
    }

    /**
     * @return The custome type of this category.
     */
    public String getCustomType() {
        return mCustomType;
    }

    /**
     * Convenience method for checking if this category has the custom "VARIABLE" type.
     *
     * @return True if this category has the custom type "VARIABLE"
     */
    public boolean isVariableCategory() {
        return mIsVariableCategory;
    }

    /**
     * Convenience method for checking if this category has the custom "FUNCTION" type.
     *
     * @return True if this category has the custom type "FUNCTION"
     */
    public boolean isFunctionCategory() {
        return mIsFunctionCategory;
    }

    /**
     * Gets the list of blocks in this category. The list should not be modified directly, instead
     * {@link #addBlock(Block)} and {@link #removeBlock(Block)} should be used.
     *
     * @return The list of blocks in this category.
     */
    public List<Block> getBlocks() {
        return mBlocks;
    }

    public List<FlyoutCategory> getSubcategories() {
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
        if (mCallback != null) {
            mCallback.onBlockAdded(mBlocks.size() - 1, block);
        }
    }

    /**
     * Add a {@link Block} to the blocks displayed in this category at the specified index.
     *
     * @param index The index to insert the block at.
     * @param block The {@link Block} to add.
     */
    public void addBlock(int index, Block block) {
        mBlocks.add(index, block);
        if (mCallback != null) {
            mCallback.onBlockAdded(index, block);
        }
    }

    /**
     * Removes a block from this category.
     *
     * @param block The block to remove.
     * @return true if the block was found and removed, false otherwise.
     */
    public boolean removeBlock(Block block) {
        int i = mBlocks.indexOf(block);
        if (i != -1) {
            mBlocks.remove(i);
            if (mCallback != null) {
                mCallback.onBlockRemoved(i, block);
            }
            return true;
        }
        return false;
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
        if (mCallback != null) {
            mCallback.onCategoryCleared();
        }
    }

    /**
     * @return True if this category contains no blocks or subcategories, false otherwise.
     */
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

    /**
     * Read the full definition of the category's contents in from XML.
     *
     * @param parser The {@link XmlPullParser} to read from.
     * @param factory The {@link BlockFactory} to use to generate blocks from their names.
     *
     * @return A new {@link FlyoutCategory} with the contents given by the XML.
     * @throws IOException when reading from the parser fails.
     * @throws XmlPullParserException when reading from the parser fails.
     */
    public static FlyoutCategory fromXml(XmlPullParser parser, BlockFactory factory)
            throws IOException, XmlPullParserException {
        FlyoutCategory result = new FlyoutCategory();
        result.mCategoryName = parser.getAttributeValue("", "name");
        result.mCustomType = parser.getAttributeValue("", "custom");
        result.mIsVariableCategory = result.mCustomType != null
                && TextUtils.equals("VARIABLE", result.mCustomType.toUpperCase());
        result.mIsFunctionCategory = result.mCustomType != null
                && TextUtils.equals("FUNCTION", result.mCustomType.toUpperCase());
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
                        result.addSubcategory(FlyoutCategory.fromXml(parser, factory));
                    } else if (parser.getName().equalsIgnoreCase("block")) {
                        result.addBlock(factory.fromXml(parser));
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

    /**
     * @param subcategory The category to add under this category.
     */
    public void addSubcategory(FlyoutCategory subcategory) {
        mSubcategories.add(subcategory);
    }


    /**
     * Callback class for listening to changes to this category.
     */
    public abstract static class Callback {
        /**
         * Called when a block is added to this category.
         *
         * @param index The index the block was added at.
         * @param block The block that was added.
         */
        public void onBlockAdded(int index, Block block) {}

        /**
         * Called when a block is removed from this category.
         *
         * @param index The index the block was previously at.
         * @param block The block that was removed.
         */
        public void onBlockRemoved(int index, Block block) {}

        /**
         * Called when the category is cleared, which removes all its subcategories and blocks.
         */
        public void onCategoryCleared() {}
    }
}
