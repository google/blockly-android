package com.google.blockly.android;

import com.google.blockly.android.ui.CategoryTabs;
import com.google.blockly.model.FlyoutCategory;

/**
 * An interface that specifies the actions that can be taken on a category selector ui component.
 */
public interface CategorySelectorUiInterface {

    /**
     * Set the root category for the category selector.
     *
     * @param rootCategory The top-level category in the toolbox.
     */
    void setContents(FlyoutCategory rootCategory);

    /**
     * Sets the currently selected category.
     * If the cateogry is not a member of the contents previously set, no category will render
     * selected.
     *
     * @param category the category to use as currently selected.
     */
    void setCurrentCategory(FlyoutCategory category);

    /**
     * @return the current category.
     */
    FlyoutCategory getCurrentCategory();

    /**
     * @param categoryCallback the callback for when the user clicks on a category.
     */
    void setCategoryCallback(CategoryTabs.Callback categoryCallback);
}
