package com.google.blockly.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.blockly.android.ui.CategoryTabs;
import com.google.blockly.model.FlyoutCategory;

/**
 * An interface that specifies the actions that can be taken on a category selector ui component.
 */
public interface CategorySelectorUI {

    /**
     * Set the root category for the category selector.
     *
     * @param rootCategory The top-level category in the toolbox.
     */
    void setContents(@NonNull FlyoutCategory rootCategory);

    /**
     * Sets the currently selected category. If {@code category} is null, or if the category is not
     * a member of the contents previously set, the current category should be unselected.
     *
     * @param category the category to use as currently selected.
     */
    void setCurrentCategory(@Nullable FlyoutCategory category);

    /**
     * @return the current category.
     */
    FlyoutCategory getCurrentCategory();

    /**
     * @param categoryCallback the callback for when the user clicks on a category.
     */
    void setCategoryCallback(@Nullable CategorySelectorUI.Callback categoryCallback);

    /**
     * Callback for when the user clicks on a category.
     */
    abstract class Callback {
        /**
         * Notifies the callback that the user has clicked on view representing a category.
         * Callback code is responsible for updating the toolbox BlockListUI (by default implemented
         * in {@link com.google.blockly.android.control.FlyoutController}).
         * @param category The clicked category.
         */
        public abstract void onCategoryClicked(@NonNull FlyoutCategory category);
    }
}
