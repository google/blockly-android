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
     * Sets the {@link BlockGroup} sequence with an output connected to this input.
     *
     * @param group The {@link BlockGroup} to connect to this input connection.
     */
    void setConnectedBlockGroup(BlockGroup group);

    /**
     * @return The {@link BlockGroup} connected to this input connection.
     */
    BlockGroup getConnectedBlockGroup();

    /**
     * Unsets the {@link BlockGroup} sequence with an output connected to this input.
     *
     * @return The previously connected {@link BlockGroup}.
     */
    // TODO(#136): Replace with setConnectedBlockGroup(null).
    BlockGroup disconnectBlockGroup();

    /**
     * Recursively disconnects the view from the model.
     */
    void unlinkModelAndSubViews();
}
