package com.google.blockly.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Interface for traversing block trees, including {@link Block}, {@link Workspace}, and eventually
 * {@link BlocklyCategory} for the toolbox and trash.
 */
public interface BlockContainer {
    /**
     * @return A unique identifying string for this instance.
     */
    @NonNull
    String getId();

    /**
     * Returns whether this class or instance always acts as a root container, and will never have a
     * parent container. This is true for {@link Workspace} and some root {@link BlocklyCategory}.
     * @return True if the instance will never have a parent. Otherwise false.
     */
    boolean isRootContainer();

    /**
     * @return The current parent container, if attached. Always null for root containers.
     */
    @Nullable
    BlockContainer getParentContainer();

    // TODO: List<Block> getChildBlocks()
}
