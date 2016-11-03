/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.model;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.blockly.utils.ColorUtils;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for a Blockly Block.
 */
public class Block {
    private static final String TAG = "Block";

    /** Array used for by {@link ColorUtils#parseColor(String, float[], int)} during I/O. **/
    private static final float[] TEMP_IO_THREAD_FLOAT_ARRAY = new float[3];

    // These values are immutable once a block is created
    private final String mUuid;
    private final String mType;
    private final int mCategory;
    private final Connection mOutputConnection;
    private final Connection mNextConnection;
    private final Connection mPreviousConnection;
    private final ArrayList<Input> mInputList;
    private final ArrayList<Connection> mConnectionList;
    private final int mColor;
    private boolean mIsShadow;

    // These values can be changed after creating the block
    private String mTooltip;
    private String mComment;
    private boolean mHasContextMenu;
    private boolean mDeletable;
    private boolean mMovable;
    private boolean mEditable;
    private boolean mCollapsed;
    private boolean mDisabled;
    private boolean mInputsInline;

    // Keep track of whether inputsInline has ever been changed.
    private boolean mInputsInlineModified = false;

    /** Position of the block in the workspace. Only serialized for the root block. */
    private WorkspacePoint mPosition;

    private Block(@Nullable String uuid, String name, int category, int color,
                  Connection outputConnection, Connection nextConnection,
                  Connection previousConnection, ArrayList<Input> inputList, boolean inputsInline,
                  boolean inputsInlineModified) {
        mUuid = (uuid != null) ? uuid : UUID.randomUUID().toString();
        mType = name;
        mCategory = category;

        // This constructor reuses Connections and Inputs instead of copying them.  Consider using
        // a BlockFactory and Builders instead of creating Blocks directly.
        mOutputConnection = outputConnection;
        mNextConnection = nextConnection;
        mPreviousConnection = previousConnection;

        mInputList = inputList;
        mInputsInline = inputsInline;
        mInputsInlineModified = inputsInlineModified;
        mPosition = new WorkspacePoint(0, 0);

        mColor = color;

        mConnectionList = new ArrayList<>();

        if (mInputList != null) {
            for (int i = 0; i < mInputList.size(); i++) {
                Input in = mInputList.get(i);
                in.setBlock(this);
                if (in.getConnection() != null) {
                    mConnectionList.add(in.getConnection());
                }
            }
        }
        if (mOutputConnection != null) {
            mOutputConnection.setBlock(this);
            mConnectionList.add(mOutputConnection);
        }
        if (mPreviousConnection != null) {
            mPreviousConnection.setBlock(this);
            mConnectionList.add(mPreviousConnection);
        }
        if (mNextConnection != null) {
            mNextConnection.setBlock(this);
            mConnectionList.add(mNextConnection);
        }
    }

    /**
     * @return The name of the block. Not for display.
     */
    public String getType() {
        return mType;
    }

    /**
     * @return The unique identifier of the block. Not for display.
     */
    public String getId() {
        return mUuid;
    }

    /**
     * Adds to <code>outList</code> all block ids for this block and all child blocks.
     * This will not include occluded shadow block ids.
     *
     * @param outList List of ids to add to.
     */
    public void addAllBlockIds(List<String> outList) {
        outList.add(getId());
        int inputCount = mInputList.size();
        for (int i = 0; i < inputCount; ++i) {
            Input input = mInputList.get(i);
            Block connectedBlock = input.getConnectedBlock();
            if (connectedBlock != null) {
                connectedBlock.addAllBlockIds(outList);
            }
        }
        if (mNextConnection != null) {
            Block next = mNextConnection.getTargetBlock();
            if (next != null) {
                next.addAllBlockIds(outList);
            }
        }
    }

    /**
     * @return The color this block should be drawn in.
     */
    public int getColor() {
        return mColor;
    }

    /**
     * @return Whether the user can edit field values.
     * @see #setEditable(boolean)
     */
    public boolean isEditable() {
        return mEditable;
    }

    /**
     * If a block is editable, the user can edit the values of the block's fields.  The editable
     * state does not affect whether child blocks can be connected or disconnected.
     *
     * Blocks are editable by default.
     *
     * Warning: FieldViews do not yet respect block disabled state (#303).
     *
     * @param editable
     */
    public void setEditable(boolean editable) {
        mEditable = editable;
    }

    /**
     * @return Whether the block can be moved.
     * @see #setMovable(boolean)
     */
    public boolean isMovable() {
        return mMovable;
    }

    /**
     * If a block is movable, users can drag it as a root block on the workspace, or disconnect it
     * from its parent block.
     *
     * Blocks are movable by default.
     *
     * @param movable
     */
    public void setMovable(boolean movable) {
        mMovable = movable;
    }

    /**
     * @return Whether the user can delete / trash this block.
     */
    public boolean isDeletable() {
        return mDeletable;
    }

    /**
     * Users can delete (i.e., drag to trash) blocks that are deletable.
     *
     * Blocks are deletable by default.
     *
     * Warning: The Android Dragger implementation does not respect deletable false state (#305).
     *
     * @param deletable
     */
    public void setDeletable(boolean deletable) {
        mDeletable = deletable;
    }

    /**
     * @return Whether the block is disabled.  Does not check if parent or ancestor is disabled.
     * @see #setDisabled(boolean)
     */
    public boolean isDisabledBlock() {
        return mDisabled;
    }

    /**
     * @return Whether the block is disabled, possibly via a disabled parent or ancestor block.
     * @see #setDisabled(boolean)
     */
    public boolean isDisabled() {
        if (mDisabled) {
            return true;
        }
        Block ancestor = getParentBlock();
        while (ancestor != null) {
            if (ancestor.mDisabled) {
                return true;
            }
            ancestor = ancestor.getParentBlock();
        }
        return false;
    }

    /**
     * Disabling a block is effectively a way to comment out a block. The block is skipped during
     * code generation, including all connection value input and statement input children.  Next
     * statement children are not effected.
     *
     * By default, blocks are not disabled.
     *
     * Warning: BlockViews do not yet render disabled state. (#306).
     *
     * @param disabled
     */
    public void setDisabled(boolean disabled) {
        mDisabled = disabled;
    }

    /**
     * @return Whether the block is collapsed.
     * @see #setCollapsed(boolean)
     */
    public boolean isCollapsed() {
        return mCollapsed;
    }

    /**
     * Users can collapse blocks, to render them in a condensed, text-only form.
     *
     * Warning: Not yet supported by Android BlockViews (#307).
     *
     * @param collapsed Whether the block should be collapsed.
     */
    public void setCollapsed(boolean collapsed) {
        mCollapsed = collapsed;
    }

    /**
     * Gets the position of this block. The position is the top left for LTR configurations and the
     * top right for RTL configurations. This position is only meaningful for top level blocks. All
     * other blocks will have a position that is dependent on the block rendering relative to the
     * top level block.
     *
     * @return The coordinates of the start corner of this block in Workspace coordinates.
     */
    public WorkspacePoint getPosition() {
        return mPosition;
    }

    /**
     * @return A list of connections (including inputs, next, previous and outputs) on this block.
     */
    public List<Connection> getAllConnections() {
        return mConnectionList;
    }

    /**
     * Add all connections on the block to the given list.
     *
     * @param addTo The list to update.
     */
    public void getAllConnections(List<Connection> addTo) {
        addTo.addAll(mConnectionList);
    }

    /**
     * Add all connections on the block and its descendants to the given list.
     *
     * @param addTo The list to update.
     */
    public void getAllConnectionsRecursive(List<Connection> addTo) {
        getAllConnections(addTo);
        for (int i = 0; i < mConnectionList.size(); i++) {
            Connection conn = mConnectionList.get(i);
            int type = conn.getType();
            Block target = conn.getTargetBlock();
            if (type != Connection.CONNECTION_TYPE_OUTPUT
                    && type != Connection.CONNECTION_TYPE_PREVIOUS
                    && target != null) {
                target.getAllConnectionsRecursive(addTo);
            }
        }
    }

    /**
     * Set the position of this block in the workspace.
     *
     * @param x The workspace x position.
     * @param y The workspace y position.
     */
    public void setPosition(int x, int y) {
        mPosition.x = x;
        mPosition.y = y;
    }

    /**
     * @return The comment on this block.
     */
    public String getComment() {
        return mComment;
    }

    /**
     * Set the comment on this block.
     *
     * @param comment The text of the comment.
     */
    public void setComment(String comment) {
        mComment = comment;
    }

    /**
     * @return The set of inputs on this block.
     */
    public List<Input> getInputs() {
        return mInputList;
    }

    /**
     * @return True if and only if this block has inputs, i.e., {#link mInputList} is non-empty.
     */
    public boolean hasInputs() {
        return !mInputList.isEmpty();
    }

    /**
     * @return Whether the flag for displaying inputs in-line has been explicitly set.
     */
    public boolean getInputsInlineModified() {
        return mInputsInlineModified;
    }

    /**
     * @return The current state of the flag for displaying inputs in-line.
     */
    public boolean getInputsInline() {
        return mInputsInline;
    }

    /**
     * Set flag for displaying inputs in-line.
     */
    public void setInputsInline(boolean inputsInline) {
        mInputsInlineModified = true;
        mInputsInline = inputsInline;
    }

    /**
     * Searches through the block's list of inputs and returns the first one with the given name.
     *
     * @param targetName The name of the input to search for.
     *
     * @return The first Input with that name.
     */
    public Input getInputByName(String targetName) {
        for (int i = 0; i < mInputList.size(); i++) {
            if (mInputList.get(i).getName() != null
                    && mInputList.get(i).getName().equalsIgnoreCase(targetName)) {
                return mInputList.get(i);
            }
        }
        return null;
    }

    /**
     * @return The only value input on the block, or null if there are zero or more than one.
     */
    public Input getOnlyValueInput() {
        Input result = null;
        for (int i = 0; i < mInputList.size(); i++) {
            if (mInputList.get(i).getType() == Input.TYPE_VALUE) {
                if (result != null) {
                    return null;    // Found more than one value input
                }
                result = mInputList.get(i);
            }
        }
        return result;
    }

    /**
     * Searches through all of the fields on all of the block's inputs.  Returns the first field
     * with the given name.
     *
     * @param targetName The name of the field to search for.
     *
     * @return The first Field with that name.
     */
    public Field getFieldByName(String targetName) {
        Input input;
        Field field;
        for (int i = 0; i < mInputList.size(); i++) {
            input = mInputList.get(i);
            for (int j = 0; j < input.getFields().size(); j++) {
                field = input.getFields().get(j);
                if (field.getName() != null && field.getName().equalsIgnoreCase(targetName)) {
                    return field;
                }
            }
        }
        return null;
    }

    /**
     * @return The block connected to this block's previous connection or null.
     */
    public Block getPreviousBlock() {
        return mPreviousConnection == null ? null : mPreviousConnection.getTargetBlock();
    }

    /**
     * @return The block connected to this block's next connection or null.
     */
    public Block getNextBlock() {
        return mNextConnection == null ? null : mNextConnection.getTargetBlock();
    }

    /**
     * @return The block's output Connection.
     */
    @Nullable
    public Connection getOutputConnection() {
        return mOutputConnection;
    }

    /**
     * @return The block's previous Connection.
     */
    @Nullable
    public Connection getPreviousConnection() {
        return mPreviousConnection;
    }

    /**
     * @return The block's next Connection.
     */
    @Nullable
    public Connection getNextConnection() {
        return mNextConnection;
    }

    /**
     * @return True if this is a shadow block, false otherwise.
     */
    public boolean isShadow() {
        return mIsShadow;
    }

    /**
     * @return True if this block can be dragged by the user, false otherwise.
     */
    public boolean isDraggable() {
        return !mIsShadow && mMovable;
    }

    /**
     * Creates a copy of this block and all inferior blocks connected to it.
     *
     * @return A new block tree with a copy of this block as the root.
     */
    public Block deepCopy() {
        // Build a copy of this block
        Block copy = new Block.Builder(this).build();

        // Build and connect a copy of the blocks attached to next
        if (mNextConnection != null) {
            copyConnection(mNextConnection, copy.mNextConnection);
        }
        // Build and connect a copy of the blocks attached to the inputs
        for (int i = 0; i < mInputList.size(); i++) {
            Input sourceInput = mInputList.get(i);
            if (sourceInput.getConnection() == null) {
                continue;
            }
            Input destInput = copy.getInputByName(sourceInput.getName());
            copyConnection(sourceInput.getConnection(), destInput.getConnection());
        }
        return copy;
    }

    /**
     * Writes information about the editable parts of the block as XML.
     *
     * @param serializer The XmlSerializer to write to.
     * @param rootBlock True if the block is a top level block, false otherwise.
     *
     * @throws IOException
     */
    public void serialize(XmlSerializer serializer, boolean rootBlock) throws IOException {
        serializer.startTag(null, mIsShadow ? "shadow" : "block")
                .attribute(null, "type", mType)
                .attribute(null, "id", mUuid);

        // The position of the block only needs to be saved if it is a top level block.
        if (rootBlock) {
            serializer.attribute(null, "x", Integer.toString(mPosition.x))
                    .attribute(null, "y", Integer.toString(mPosition.y));
        }

        if (isCollapsed()) {
            serializer.attribute(null, "collapsed", "true");
        }
        if (!isDeletable() && !isShadow()) {
            serializer.attribute(null, "deletable", "false");
        }
        if (isDisabled()) {
            serializer.attribute(null, "disabled", "true");
        }
        if (!isEditable()) {
            serializer.attribute(null, "editable", "false");
        }
        if (!isMovable() && !isShadow()) {
            serializer.attribute(null, "movable", "false");
        }

        // Only serialize whether the inputs are internal or external if it has been explicitly
        // modified.  This can happen if it's set explicitly in json, if it's set in xml, or if
        // it was changed by the user in the program.
        if (mInputsInlineModified) {
            serializer.attribute(null, "inline", Boolean.toString(mInputsInline));
        }

        for (int i = 0; i < mInputList.size(); i++) {
            if (mInputList.get(i) != null) {
                mInputList.get(i).serialize(serializer);
            }
        }

        if (getNextBlock() != null) {
            serializer.startTag(null, "next");
            getNextBlock().serialize(serializer, false);
            serializer.endTag(null, "next");
        }

        serializer.endTag(null, mIsShadow ? "shadow" : "block");
    }

    /**
     * @return The {@link Block} for the last non-shadow child in this sequence, possibly itself.
     */
    public Block getLastBlockInSequence() {
        Block last = this;
        Block next = this.getNextBlock();
        // Protect against loops by checking for dupes?
        while (next != null && !next.isShadow()) {
            last = next;
            next = last.getNextBlock();
        }
        return last;
    }

    /**
     * Walks the chain of blocks in this block, at each stage checking if there are multiple
     * value inputs.  If there is only one value input at each block, follows that input to the
     * next block.  If at any point there is more than one value input on a block or there are no
     * value inputs on a block, returns null. If the next block in the sequence is a shadow block
     * this returns the connection before the shadow.
     *
     * @return the {@link Connection} on the only input on the last block in the chain.
     */
    public Connection getLastUnconnectedInputConnection() {
        Block block = this;

        // Loop until there are no more singular, connected inputs.
        while (true) {
            Input onlyValueInput = block.getOnlyValueInput();
            if (onlyValueInput == null) {
                return null;
            }
            Connection conn = onlyValueInput.getConnection();
            if (conn == null) {
                return null;
            }
            if (!conn.isConnected()) {
                return conn;
            }
            block = conn.getTargetBlock();
            if (block.isShadow()) {
                return conn;
            }
        }
    }

    /**
     * Find the highest block in the hierarchy that this {@link Block} descends from.
     *
     * @return The highest block found.
     */
    public Block getRootBlock() {
        Block block = this;
        Block parent = block.getParentBlock();
        // Go up and left as far as possible.
        while (parent != null) {
            block = parent;
            parent = block.getParentBlock();
        }
        return block;
    }

    /**
     * @return The previous or output {@link Connection} on this block, or null.
     */
    @Nullable
    public Connection getUpwardsConnection() {
        if (mPreviousConnection != null) {
            return mPreviousConnection;
        }
        if (mOutputConnection != null) {
            return mOutputConnection;
        }
        return null;
    }

    /**
     * @return The next or input {@link Connection} this block is connected to, or null.
     */
    @Nullable
    public Connection getParentConnection() {
        Connection upwards = getUpwardsConnection();
        return upwards == null ? null : upwards.getTargetConnection();
    }

    /**
     * @return The block connected to the output or previous {@link Connection}, if present.
     *         Otherwise null.
     */
    public Block getParentBlock() {
        Connection parentConnection = getParentConnection();
        return parentConnection == null ? null : parentConnection.getBlock();
    }

    /**
     * Configures whether this block should be a shadow block. This should only be called during
     * creation of the block.
     *
     * @param isShadow
     */
    void setShadow(boolean isShadow) {
        if (mIsShadow == isShadow) {
            return;
        }

        // Unsupported: Changing while connected to the parent (Connection target type, etc.)
        Connection upConnection = getUpwardsConnection();
        if (upConnection != null && upConnection.getTargetConnection() != null) {
            throw new IllegalStateException(
                    "Cannot change block shadow state while connected to parent.");
        }
        if (isShadow) {
            if (containsVariableField(mInputList)) {
                throw new IllegalStateException("Shadow blocks cannot contain variable fields.");
            }

            // Shadow blocks cannot have non-shadow children.
            int inputCount = mInputList == null ? 0 : mInputList.size();
            for (int i = 0; i < inputCount; ++i) {
                Input input = mInputList.get(i);
                Block child = input.getConnectedBlock();  // Not the shadow connection.
                if (child != null && !child.isShadow()) {
                    throw new IllegalStateException(
                            "Cannot change block to shadow while non-shadow children are connected."
                    );
                }
            }
        }

        // State change is valid. Proceed.
        mIsShadow = isShadow;
    }

    /**
     * Makes a copy of any blocks connected to the source connection and adds the copies to the
     * destination connection. The source and destination Connections must be of the same type and
     * must be either a next or input connection.
     *
     * @param sourceConnection The connection to copy blocks from.
     * @param destConnection The connection to add copied blocks to.
     */
    private void copyConnection(Connection sourceConnection, Connection destConnection) {
        if (sourceConnection.getType() != destConnection.getType() ||
                (sourceConnection.getType() != Connection.CONNECTION_TYPE_NEXT &&
                        sourceConnection.getType() != Connection.CONNECTION_TYPE_INPUT)) {
            throw new IllegalArgumentException(
                    "Connection types must match and must be a superior connection.");
        }
        Block copy = null;
        if (sourceConnection.getShadowBlock() != null) {
            // Make a copy of the shadow if we have one and set it on the connection
            copy = sourceConnection.getShadowBlock().deepCopy();
            if (destConnection.getType() == Connection.CONNECTION_TYPE_NEXT) {
                destConnection.setShadowConnection(copy.getPreviousConnection());
            } else if (destConnection.getType() == Connection.CONNECTION_TYPE_INPUT) {
                destConnection.setShadowConnection(copy.getOutputConnection());
            }
        }

        if (sourceConnection.getTargetBlock() != null) {
            // If a block other than the shadow was connected make a copy of that
            if (sourceConnection.getTargetBlock() != sourceConnection.getShadowBlock()) {
                copy = sourceConnection.getTargetBlock().deepCopy();
            }
            // Connect a copy of whichever block was connected to the source
            if (destConnection.getType() == Connection.CONNECTION_TYPE_NEXT) {
                destConnection.connect(copy.getPreviousConnection());
            } else if (destConnection.getType() == Connection.CONNECTION_TYPE_INPUT) {
                destConnection.connect(copy.getOutputConnection());
            }
        }
    }

    /**
     * Breaks a block message up into args and text. The returned Strings should all either
     * exactly match "^%\\d+$" if they are an arg or else are just text for a label. %[0-9]+ will
     * always be treated as an argument regardless of where in the string it appears, unless the
     * % is escaped (eg "Escaped %%5 has no args")
     *
     * @param message The message to tokenize.
     *
     * @return A list of Strings that are either an arg or plain text.
     */
    /*package*/
    static List<String> tokenizeMessage(String message) {
        ArrayList<String> result = new ArrayList<>();
        if (TextUtils.isEmpty(message)) {
            return result;
        }
        boolean foundPercent = false;
        int lastSplit = 0;
        int lastPercent = -1;

        for (int i = 0, length = message.length(); i < length; i++) {
            char currChar = message.charAt(i);
            if (currChar == '%') {
                if (i + 1 < length) {
                    char nextChar = message.charAt(i + 1);
                    if (nextChar == '%' || !Character.isDigit(nextChar)) {
                        // If we have %% or this is not an arg don't pull it out of the string.
                        i++;
                        continue;
                    }
                } else {
                    // Done processing the string. Final % will be included in the last token.
                    continue;
                }
                foundPercent = true;
                lastPercent = i;
            } else if (foundPercent) {
                if (Character.isDigit(currChar)) {
                    continue;
                } else {
                    String potentialText = message.substring(lastSplit, lastPercent).trim();
                    if (!TextUtils.isEmpty(potentialText)) {
                        result.add(potentialText);
                    }
                    result.add(message.substring(lastPercent, i));
                    lastSplit = i;
                    foundPercent = false;
                }
            }
        }
        if (lastSplit != message.length() - 1) {
            // We have remaining pieces to split
            if (lastPercent > lastSplit) {
                String potentialText = message.substring(lastSplit, lastPercent).trim();
                if (!TextUtils.isEmpty(potentialText)) {
                    result.add(potentialText);
                }
                result.add(message.substring(lastPercent, message.length()));
            } else {
                String potentialText = message.substring(lastSplit, message.length()).trim();
                if (!TextUtils.isEmpty(potentialText)) {
                    result.add(potentialText);
                }
            }
        }

        return result;
    }

    /**
     * @return True if any input in this block includes a variable field. Otherwise false.
     */
    boolean containsVariableField() {
        return containsVariableField(mInputList);
    }

    /**
     * @param inputs {@link Input}s to check for variable fields.
     * @return True if any input in {@code inputs} includes a variable field. Otherwise false.
     */
    private static boolean containsVariableField(ArrayList<Input> inputs) {
        // Verify there's not a variable field
        int inputCount = inputs.size();
        for (int i = 0; i < inputCount; i++) {
            Input in = inputs.get(i);
            List<Field> fields = in.getFields();
            int fieldCount = fields.size();
            for (int j = 0; j < fieldCount; j++) {
                Field f = fields.get(j);
                if (f.getType() == Field.TYPE_VARIABLE) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class Builder {
        private final WorkspacePoint mPosition;
        // These values are immutable once a block is created
        private String mUuid;
        private String mType;
        private int mCategory;
        private int mColor = ColorUtils.DEFAULT_BLOCK_COLOR;
        private Connection mOutputConnection;
        private Connection mNextConnection;
        private Connection mPreviousConnection;
        private ArrayList<Input> mInputs;
        // These values can be changed after creating the block
        private String mTooltip;
        private String mComment;
        private boolean mHasContextMenu = false;
        private boolean mInputsInline = false;
        private boolean mInputsInlineModified = false;
        private boolean mIsShadow = false;
        private boolean mDeletable = true;
        private boolean mMovable = true;
        private boolean mEditable = true;
        private boolean mCollapsed = false;
        private boolean mDisabled = false;

        public Builder(String type) {
            mType = type;
            mInputs = new ArrayList<>();
            mPosition = new WorkspacePoint(0, 0);
        }

        public Builder(Block block) {
            this(block.mType);
            mColor = block.mColor;
            mCategory = block.mCategory;

            mOutputConnection = Connection.cloneConnection(block.mOutputConnection);
            mNextConnection = Connection.cloneConnection(block.mNextConnection);
            mPreviousConnection = Connection.cloneConnection(block.mPreviousConnection);

            mInputs = new ArrayList<>();
            Input newInput;
            for (int i = 0; i < block.mInputList.size(); i++) {
                Input oldInput = block.mInputList.get(i);
                newInput = oldInput.clone();
                if (newInput != null) {
                    mInputs.add(newInput);
                }
            }

            mInputsInline = block.mInputsInline;
            mInputsInlineModified = block.mInputsInlineModified;

            // TODO: Reconsider the defaults for these
            mTooltip = block.mTooltip;
            mComment = block.mComment;
            mHasContextMenu = block.mHasContextMenu;
            mIsShadow = block.mIsShadow;
            mDeletable = block.mDeletable;
            mMovable = block.mMovable;
            mEditable = block.mEditable;
            mCollapsed = block.mCollapsed;
            mDisabled = block.mDisabled;
            mPosition.x = block.mPosition.x;
            mPosition.y = block.mPosition.y;
        }

        public Builder setType(String type) {
            mType = type;
            return this;
        }

        public Builder setUuid(String uuid) {
            mUuid = uuid;
            return this;
        }

        public Builder setColorHue(int hue) {
            mColor = ColorUtils.getBlockColorForHue(hue, TEMP_IO_THREAD_FLOAT_ARRAY);
            return this;
        }

        public Builder setColor(int color) {
            mColor = color;
            return this;
        }

        public Builder setCategory(int category) {
            mCategory = category;
            return this;
        }

        public Builder setOutput(Connection outputConnection) {
            if (this.mPreviousConnection != null) {
                throw new IllegalStateException(
                        "Block cannot have both output and previous connection.");
            }
            this.mOutputConnection = outputConnection;
            return this;
        }

        public Builder setNext(Connection nextConnection) {
            this.mNextConnection = nextConnection;
            return this;
        }

        public Builder setPrevious(Connection previousConnection) {
            if (this.mOutputConnection != null) {
                throw new IllegalStateException(
                        "Block cannot have both previous and output connection.");
            }
            this.mPreviousConnection = previousConnection;
            return this;
        }

        public Builder addInput(Input input) {
            mInputs.add(input);
            return this;
        }

        public Builder setInputs(ArrayList<Input> inputs) {
            if (inputs == null) {
                throw new IllegalArgumentException("Inputs may not be null.");
            }
            this.mInputs = inputs;
            return this;
        }

        public Builder setInputsInline(boolean inputsInline) {
            this.mInputsInline = inputsInline;
            this.mInputsInlineModified = true;
            return this;
        }

        public Builder setTooltip(String tooltip) {
            mTooltip = tooltip;
            return this;
        }

        public Builder setComment(String comment) {
            mComment = comment;
            return this;
        }

        public Builder setHasContextMenu(boolean hasContextMenu) {
            mHasContextMenu = hasContextMenu;
            return this;
        }

        public Builder setShadow(boolean isShadow) {
            mIsShadow = isShadow;
            return this;
        }

        public Builder setDeletable(boolean canDelete) {
            mDeletable = canDelete;
            return this;
        }

        public Builder setMovable(boolean canMove) {
            mMovable = canMove;
            return this;
        }

        public Builder setEditable(boolean canEdit) {
            mEditable = canEdit;
            return this;
        }

        public Builder setCollapsed(boolean collapsed) {
            mCollapsed = collapsed;
            return this;
        }

        public Builder setDisabled(boolean disabled) {
            mDisabled = disabled;
            return this;
        }

        public Builder setPosition(int x, int y) {
            mPosition.x = x;
            mPosition.y = y;
            return this;
        }

        public Block build() {
            if (mIsShadow && containsVariableField(mInputs)) {
                throw new IllegalArgumentException("Shadow blocks cannot contain variables");
            }
            Block b = new Block(mUuid, mType, mCategory, mColor, mOutputConnection, mNextConnection,
                    mPreviousConnection, mInputs, mInputsInline, mInputsInlineModified);
            b.mTooltip = mTooltip;
            b.mComment = mComment;
            b.mHasContextMenu = mHasContextMenu;
            b.mIsShadow = mIsShadow;
            b.mDeletable = mDeletable;
            b.mMovable = mMovable;
            b.mEditable = mEditable;
            b.mCollapsed = mCollapsed;
            b.mDisabled = mDisabled;
            b.mPosition = mPosition;

            return b;
        }

    }

}
