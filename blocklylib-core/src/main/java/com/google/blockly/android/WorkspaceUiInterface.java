package com.google.blockly.android;

import com.google.blockly.android.control.BlocklyController;

/**
 * An interface that specifies the actions that can be taken on a workspace ui component.
 */
public interface WorkspaceUiInterface {

    /**
     * Sets the controller to use in this fragment for instantiating views. This should be the same
     * controller used for any associated {@link FlyoutFragment FlyoutFragments}.
     *
     * @param controller The controller backing this fragment.
     */
    void setController(BlocklyController controller);
}
