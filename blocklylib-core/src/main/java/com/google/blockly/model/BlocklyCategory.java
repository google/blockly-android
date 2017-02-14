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

import android.support.annotation.IntDef;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.Log;

import com.google.blockly.android.FlyoutFragment;
import com.google.blockly.utils.ColorUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * A category of a toolbox, which holds zero or more blocks or zero or more subcategories. Not both.
 * {@link FlyoutFragment} is responsible for displaying this.
 */
public class BlocklyCategory {
    private static final String TAG = "BlocklyCategory";

    public static final SimpleArrayMap<String, CategoryFactory> CATEGORY_FACTORIES
            = new SimpleArrayMap<>();

    /** Array used for by {@link ColorUtils#parseColor(String, float[], int)} during I/O. **/
    private static final float[] TEMP_IO_THREAD_FLOAT_ARRAY = new float[3];

    protected final List<BlocklyCategory> mSubcategories = new ArrayList<>();
    protected final List<CategoryItem> mItems = new ArrayList<>();
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
     * {@link #addItem(CategoryItem)} and {@link #removeItem(CategoryItem)} should be used.
     *
     * @return The list of blocks in this category.
     */
    public List<CategoryItem> getItems() {
        return mItems;
    }

    public List<BlocklyCategory> getSubcategories() {
        return mSubcategories;
    }

    public Integer getColor() {
        return mColor;
    }

    /**
     * Add a {@link Block} to the blocks displayed in this category.
     *
     * @param item The {@link Block} to add.
     */
    public void addItem(CategoryItem item) {
        mItems.add(item);
        if (mCallback != null) {
            mCallback.onItemAdded(mItems.size() - 1, item);
        }
    }

    /**
     * Add a {@link Block} to the blocks displayed in this category at the specified index.
     *
     * @param index The index to insert the block at.
     * @param item The {@link Block} to add.
     */
    public void addItem(int index, CategoryItem item) {
        mItems.add(index, item);
        if (mCallback != null) {
            mCallback.onItemAdded(index, item);
        }
    }

    /**
     * Removes an item from this category.
     *
     * @param item The item to remove.
     * @return true if the item was found and removed, false otherwise.
     */
    public boolean removeItem(CategoryItem item) {
        int i = mItems.indexOf(item);
        if (i != -1) {
            return removeItem(i);
        }
        return false;
    }

    /**
     * Removes an item from this category.
     *
     * @param index The position of the item to remove.
     * @return true if the item was removed, otherwise an OOBE will be thrown.
     */
    public boolean removeItem(int index) {
        CategoryItem item = mItems.remove(index);
        if (mCallback != null) {
            mCallback.onItemRemoved(index, item);
        }
        return true;
    }

    /**
     * Convenience method for removing a {@link BlockItem} from this category by its block.
     *
     * @param block The block to locate and remove.
     * @return true if an item with that block was found and removed, false otherwise.
     */
    public boolean removeBlock(Block block) {
        for (int i = 0; i < mItems.size(); i++) {
            CategoryItem item = mItems.get(i);
            if (item.getType() == CategoryItem.TYPE_BLOCK) {
                Block currBlock = ((BlockItem)item).getBlock();
                if (currBlock == block) {
                    return removeItem(i);
                }
            }
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
        mItems.clear();
        mSubcategories.clear();
        if (mCallback != null) {
            mCallback.onCategoryCleared();
        }
    }

    /**
     * @return True if this category contains no blocks or subcategories, false otherwise.
     */
    public boolean isEmpty() {
        return mSubcategories.isEmpty() && mItems.isEmpty();
    }

    /**
     * Fill the given list with of the {@link Block} instances in this category and its
     * subcategories.
     *
     * @param blocks The list to add to, which is not cleared before adding blocks.
     */
    public void getAllBlocksRecursive(List<Block> blocks) {
        for (CategoryItem item : mItems) {
            if (item.getType() == CategoryItem.TYPE_BLOCK) {
                blocks.add(((BlockItem) item).getBlock());
            }
        }
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
     * @return A new {@link BlocklyCategory} with the contents given by the XML.
     * @throws IOException when reading from the parser fails.
     * @throws XmlPullParserException when reading from the parser fails.
     */
    public static BlocklyCategory fromXml(XmlPullParser parser, BlockFactory factory)
            throws IOException, XmlPullParserException {
        BlocklyCategory result;
        String customType = parser.getAttributeValue("", "custom");
        if (CATEGORY_FACTORIES.containsKey(customType)) {
            result = CATEGORY_FACTORIES.get(customType).obtainCategory(customType);
        } else {
            result = new BlocklyCategory();
        }
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
                        result.addSubcategory(BlocklyCategory.fromXml(parser, factory));
                    } else if (parser.getName().equalsIgnoreCase("block")) {
                        result.addItem(new BlockItem(factory.fromXml(parser)));
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
    public void addSubcategory(BlocklyCategory subcategory) {
        mSubcategories.add(subcategory);
    }


    /**
     * Callback class for listening to changes to this category.
     */
    public abstract static class Callback {
        /**
         * Called when an item is added to this category.
         *
         * @param index The index the item was added at.
         * @param item The item that was added.
         */
        public void onItemAdded(int index, CategoryItem item) {}

        /**
         * Called when an item is removed from this category.
         *
         * @param index The index the item was previously at.
         * @param item The item that was removed.
         */
        public void onItemRemoved(int index, CategoryItem item) {}

        /**
         * Called when the category is cleared, which removes all its subcategories and items.
         */
        public void onCategoryCleared() {}
    }

    /**
     * Wraps items that can be displayed as part of a {@link BlocklyCategory}.
     */
    public abstract static class CategoryItem {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({TYPE_BLOCK, TYPE_LABEL, TYPE_BUTTON})
        public @interface ItemType {
        }
        public static final int TYPE_BLOCK = 0;
        public static final int TYPE_LABEL = 1;
        public static final int TYPE_BUTTON = 2;

        private final @ItemType int mType;

        public CategoryItem(@ItemType int type) {
            mType = type;
        }

        public @CategoryItem.ItemType int getType() {
            return mType;
        }
    }

    /**
     * Flyout item that contains a stack blocks.
     */
    public static class BlockItem extends CategoryItem {
        private final Block mBlock;

        public BlockItem(Block block) {
            super(TYPE_BLOCK);
            mBlock = block;
        }

        public Block getBlock() {
            return mBlock;
        }
    }

    /**
     * Flyout item representing a clickable button, such as "Add Variable".
     * TODO (#503): Support style and callback spec
     */
    public static class ButtonItem extends CategoryItem {
        private final String mText;
        private final String mAction;
        public ButtonItem(String text, String action) {
            super(TYPE_BUTTON);
            mText = text;
            mAction = action;
        }

        public String getText() {
            return mText;
        }

        public String getAction() {
            return mAction;
        }
    }

    /**
     * Flyout item representing a label between groups of blocks.
     * TODO (#503): Support styling
     */
    public static class LabelItem extends CategoryItem {
        private final String mText;

        public LabelItem(String text) {
            super(TYPE_LABEL);
            mText = text;
        }

        public String getText() {
            return mText;
        }
    }
}
