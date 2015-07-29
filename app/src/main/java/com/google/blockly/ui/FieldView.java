package com.google.blockly.ui;

/**
 * Describes methods that all views that are representing a Field must implement.
 */
public interface FieldView {
    /**
     * Gets the height of this view as part of a block, not including any extra panels or dropdowns
     * that are currently open.
     *
     * @return The height the view should take up as part of a block.
     */
    public int getInBlockHeight();

    /**
     * Gets the width of this view as part of a block, not including any extra panels or dropdowns
     * that are currently open.
     *
     * @return The width the view should take up as part of a block.
     */
    public int getInBlockWidth();
}
