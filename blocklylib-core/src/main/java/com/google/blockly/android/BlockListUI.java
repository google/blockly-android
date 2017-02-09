package com.google.blockly.android;

import android.support.annotation.Nullable;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.FlyoutCallback;
import com.google.blockly.model.FlyoutCategory;

/**
 * An interface that specifies the actions that can be taken on a flyout ui component.
 */
public interface BlockListUI {

    /**
     * @return True if this UI is currently visible, false otherwise.
     */
    boolean isOpen();

    /**
     * Sets the Flyout's current {@link FlyoutCategory}, including opening or closing the drawer.
     * In closeable toolboxes, {@code null} {@code category} is equivalent to closing the drawer.
     * Otherwise, the drawer will be rendered empty.
     *
     * @param category The {@link FlyoutCategory} with blocks to display.
     */
    void setCurrentCategory(@Nullable FlyoutCategory category);

    /**
     * @return True if this flyout is allowed to close, false otherwise.
     */
    boolean isCloseable();

    /**
     * Connects the {@link BlockListUI} to the application's drag and click handling. It is
     * called by
     * {@link BlocklyController#setToolboxUi(BlockListUI, CategorySelectorUI)}
     * and should not be called by the application developer.
     *
     * @param callback The callback that will handle user actions in the flyout.
     */
    void init(BlocklyController controller, FlyoutCallback callback);

    /**
     * Attempts to hide or close the blocks UI (e.g., a drawer).
     *
     * @return True if an action was taken (the drawer is closeable and was previously open).
     */
    boolean closeUi();
}
