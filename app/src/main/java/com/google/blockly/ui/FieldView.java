package com.google.blockly.ui;

/**
 * Describes methods that all views that are representing a Field must implement.
 */
public interface FieldView {
    /**
     * Gets the height of this view as part of a block, not including any extra panels or dropdowns
     * that are currently open. This is the height in the block.
     *
     * @return The height the view should take up in a block.
     */
    public int getInBlockHeight();

    /**
     * Gets the width of this view as part of a block, not including any extra panels or dropdowns
     * that are currently open. This is the width in the block.
     *
     * @return The width the view should take up in a block.
     */
    public int getInBlockWidth();
}
