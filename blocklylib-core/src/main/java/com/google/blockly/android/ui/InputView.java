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
     * hierarchy.
     *
     * @param group The {@link BlockGroup} to add to this input view.
     */
    void setConnectedBlockGroup(BlockGroup group);

    /**
     * @return The {@link BlockGroup} connected to this input connection.
     */
    BlockGroup getConnectedBlockGroup();

    /**
     * Unsets the {@link BlockGroup} in this input view and updates the view hierarchy.
     *
     * @return The previously set {@link BlockGroup}.
     */
    // TODO(#136): Replace with setConnectedBlockGroup(null).
    BlockGroup disconnectBlockGroup();

    /**
     * Recursively disconnects the view from the model, including all subviews/model subcomponents.
     * <p/>
     * This method should only be called by {@link BlockView#unlinkModel()}.
     */
    void unlinkModel();
}
