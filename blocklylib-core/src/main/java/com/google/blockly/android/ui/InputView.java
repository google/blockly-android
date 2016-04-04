package com.google.blockly.android.ui;

import android.view.ViewGroup;

import com.google.blockly.model.Input;

/**
 * Abstract methods required by {@link Input} views.
 * <p/>
 * Implementations of {@link InputView} must extend {@link ViewGroup} or one of its subclasses. The
 * class should also disable activated/focused/pressed/selected state propegation, as implemented in
 * {@link NonPropagatingViewGroup}.
 */
public interface InputView {
    /**
     * @return The {@link Input} represented by this view.
     */
    Input getInput();

    /**
     * Sets the {@link BlockGroup} containing the block connected to this input and updates the view
     * hierarchy. Calling this with null will disconnect the current group.
     *
     * @param group The {@link BlockGroup} to add to this input view.
     */
    void setConnectedBlockGroup(BlockGroup group);

    /**
     * Sets the {@link BlockGroup} containing the shadow block connected to this input and updates
     * the view hierarchy. This generally only needs to be done when the view hierarchy is first
     * created. Calling this with null will disconnect the current group.
     *
     * @param group The {@link BlockGroup} to add to this input view.
     */
    void setConnectedShadowGroup(BlockGroup group);

    /**
     * @return The {@link BlockGroup} connected to this input connection.
     */
    BlockGroup getConnectedBlockGroup();

    /**
     * @return The {@link BlockGroup} shadow block connected to this input connection.
     */
    BlockGroup getConnectedShadowGroup();

    /**
     * Recursively disconnects the view from the model, including all subviews/model subcomponents.
     * <p/>
     * This method should only be called by {@link BlockView#unlinkModel()}.
     */
    void unlinkModel();
}
