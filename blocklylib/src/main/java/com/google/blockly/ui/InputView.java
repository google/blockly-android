package com.google.blockly.ui;

import com.google.blockly.model.Input;

/**
 * Abstract methods required by {@link Input} views.
 */
public interface InputView {
    Input getInput();

    void setConnectedBlockGroup(BlockGroup group);

    BlockGroup getConnectedBlockGroup();

    BlockGroup disconnectBlockGroup();

    void unlinkModelAndSubViews();
}
