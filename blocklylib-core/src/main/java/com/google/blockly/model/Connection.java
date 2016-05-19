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

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.google.blockly.android.ui.InputView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describes a connection on a Block. This can be a previous/next connection, an output, or
 * the connection on an {@link Input}.
 */
public class Connection implements Cloneable {
    private static final String TAG = "Connection";
    /**
     * A previous connection on a block. May only be connected to a next connection. A block may
     * not have both a previous and an output connection. This is only used for the previous link
     * on a block.
     */
    public static final int CONNECTION_TYPE_PREVIOUS = 0;
    /**
     * A next connection on a block. May only be connected to a previous connection. This is used
     * for the next link on a block and for inputs that connect to a set of statement blocks.
     */
    public static final int CONNECTION_TYPE_NEXT = 1;
    /**
     * An input connection on a block. May only be connected to an output connection. This is used
     * by inputs that take a single value block.
     */
    public static final int CONNECTION_TYPE_INPUT = 2;
    /**
     * An output connection on a block. May only be connected to an input connection. A block may
     * not have both an output and a previous connection. This is only used for the output link on
     * a block.
     */
    public static final int CONNECTION_TYPE_OUTPUT = 3;
    public static final int CAN_CONNECT = 0;
    public static final int REASON_SELF_CONNECTION = 1;
    public static final int REASON_WRONG_TYPE = 2;
    public static final int REASON_MUST_DISCONNECT = 3;
    public static final int REASON_TARGET_NULL = 4;
    public static final int REASON_CHECKS_FAILED = 5;

    // If updating this, also update ConnectionManager's matchingLists and oppositeLists arrays.
    @ConnectionType
    private static final int[] OPPOSITE_TYPES = new int[]{
            CONNECTION_TYPE_NEXT, // PREVIOUS -> NEXT
            CONNECTION_TYPE_PREVIOUS, // NEXT -> PREVIOUS
            CONNECTION_TYPE_OUTPUT, // INPUT -> OUTPUT
            CONNECTION_TYPE_INPUT // OUTPUT -> INPUT
    };
    @ConnectionType
    private final int mConnectionType;
    private final String[] mConnectionChecks;
    /**
     * Position of the connection in the workspace, used by the connection manager. The position is
     * not a part of the serialized model, and is only updated when connected to a view.
     */
    private final WorkspacePoint mPosition = new WorkspacePoint();

    private Block mBlock;
    private Input mInput;
    private Connection mTargetConnection;
    // The shadow connection is only valid for next/input connections. It is not a live connection,
    // just a reference to the default shadow (if there is one) that should be connected when there
    // isn't another connection.
    private Connection mTargetShadowConnection;
    private boolean mInDragMode = false;

    public Connection(@ConnectionType int type, String[] checks) {
        mConnectionType = type;
        mConnectionChecks = checks;
    }

    /**
     * @return A new Connection with the same type and checks but with null block,
     * input and target connection.
     */
    @Override
    public Connection clone() {
        return new Connection(this.getType(), this.getConnectionChecks());
    }

    /**
     * Check if this can be connected to the target connection.
     *
     * @param target The connection to check.
     *
     * @return True if the target can be connected, false otherwise.
     */
    public boolean canConnect(Connection target) {
        return canConnectWithReason(target) == CAN_CONNECT;
    }

    /**
     * Sets the connection (and shadow block) to use when a normal block isn't connected. This may
     * only be called on connections that belong to an input (value or statement).
     *
     * @param target The connection on the shadow block to use.
     */
    public void setShadowConnection(Connection target) {
        if (target == null) {
            mTargetShadowConnection = null;
            return;
        }
        if (canConnectWithReason(target, true) != CAN_CONNECT) {
            throw new IllegalArgumentException("The shadow connection can't be connected.");
        }
        if (!target.getBlock().isShadow()) {
            throw new IllegalArgumentException("The connection does not belong to a shadow block");
        }
        mTargetShadowConnection = target;
    }

    /**
     * Connect this to another connection. If the connection is not valid a {@link RuntimeException}
     * will be thrown.
     *
     * @param target The connection to connect to.
     */
    public void connect(Connection target) {
        if (target == mTargetConnection) {
            return;
        }
        checkConnection(target);
        connectInternal(target);
        target.connectInternal(this);
    }

    /**
     * Removes the connection between this and the Connection this is connected to. If this is not
     * connected disconnect() does nothing.
     */
    public void disconnect() {
        if (mTargetConnection == null) {
            return;
        }
        Connection target = mTargetConnection;
        disconnectInternal();
        target.disconnectInternal();
    }

    public void setDragMode(boolean dragMode) {
        mInDragMode = dragMode;
    }

    public boolean inDragMode() {
        return mInDragMode;
    }

    /**
     * @return The {@link Block} this is connected to or null if it is not connected.
     */
    public Block getTargetBlock() {
        return mTargetConnection == null ? null : mTargetConnection.getBlock();
    }

    /**
     * @return The shadow {@link Block} that is used when no other block is connected or null if it
     * has no shadow block.
     */
    public Block getShadowBlock() {
        return mTargetShadowConnection == null ? null : mTargetShadowConnection.getBlock();
    }

    /**
     * @return The shadow block {@link Connection} that is used when no other block is connected or
     * null if it has no shadow block.
     */
    public Connection getShadowConnection() {
        return mTargetShadowConnection;
    }

    /**
     * Gets the set of checks for this connection. Two Connections may be connected if one of them
     * supports any connection (when this is null) or if they share at least one common check
     * value. For example, {"Number", "Integer", "MyValueType"} and {"AnotherType", "Integer"} would
     * be valid since they share "Integer" as a check.
     *
     * @return The set of checks for this connection.
     */
    public String[] getConnectionChecks() {
        return mConnectionChecks;
    }

    /**
     * Gets the type of this connection. Valid types are:
     * <ul>
     * <li>{@link #CONNECTION_TYPE_PREVIOUS}</li>
     * <li>{@link #CONNECTION_TYPE_NEXT}</li>
     * <li>{@link #CONNECTION_TYPE_INPUT}</li>
     * <li>{@link #CONNECTION_TYPE_OUTPUT}</li>
     * </ul>
     *
     * @return The type of this connection.
     */
    @ConnectionType
    public int getType() {
        return mConnectionType;
    }

    /**
     * @return The Block this connection belongs to.
     */
    public Block getBlock() {
        return mBlock;
    }

    /**
     * Sets the block that this connection is part of.
     */
    public void setBlock(Block block) {
        mBlock = block;
    }

    /**
     * @return The Input for this connection or null.
     */
    public Input getInput() {
        return mInput;
    }

    /**
     * Sets the input this connection is part of.
     */
    public void setInput(Input input) {
        mInput = input;
    }

    /**
     * @return The Connection this is connected to.
     */
    public Connection getTargetConnection() {
        return mTargetConnection;
    }

    /**
     * Sets the position of this connection in the workspace. An input position only makes sense as
     * part of a rendered block, but workspace coordinates are used to avoid having to update every
     * connection when the scaling of the view changes.
     *
     * @param x The x position in workspace coordinates.
     * @param y The y position in workspace coordinates.
     */
    public void setPosition(int x, int y) {
        mPosition.x = x;
        mPosition.y = y;
    }

    /**
     * Returns the distance between this connection and another connection.
     *
     * @param other The other {@link Connection} to measure the distance to.
     *
     * @return The distance between connections.
     */
    public double distanceFrom(Connection other) {
        int xDiff = mPosition.x - other.getPosition().x;
        int yDiff = mPosition.y - other.getPosition().y;
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    /**
     * @return The input's position in workspace coordinates.
     */
    public WorkspacePoint getPosition() {
        return mPosition;
    }

    /**
     * @return True if the target connection is non-null, false otherwise.
     */
    public boolean isConnected() {
        return mTargetConnection != null;
    }

    /**
     * @return Whether the connection has high priority in the context of bumping connections away.
     */
    public boolean isHighPriority() {
        return mConnectionType == CONNECTION_TYPE_INPUT || mConnectionType == CONNECTION_TYPE_NEXT;
    }

    /**
     * @param target The {@link Connection} to check compatibility with.
     *
     * @return {@code CAN_CONNECT} if the connection is legal, an error code otherwise.
     */
    @CheckResultType
    public int canConnectWithReason(Connection target) {
        return canConnectWithReason(target, false);
    }

    /**
     * @param target The {@link Connection} to check compatibility with.
     * @param ignoreDisconnect True to skip checking if the connection is already connected.
     *
     * @return {@code CAN_CONNECT} if the connection is legal, an error code otherwise.
     */
    @CheckResultType
    private int canConnectWithReason(Connection target, boolean ignoreDisconnect) {
        if (target == null || target.getBlock() == null) {
            return REASON_TARGET_NULL;
        }
        if (getBlock() != null && target.getBlock() == getBlock()) {
            return REASON_SELF_CONNECTION;
        }
        if (target.getType() != OPPOSITE_TYPES[mConnectionType]) {
            return REASON_WRONG_TYPE;
        }
        if (!ignoreDisconnect && mTargetConnection != null) {
            return REASON_MUST_DISCONNECT;
        }
        if (!checksMatch(target)) {
            return REASON_CHECKS_FAILED;
        }
        return CAN_CONNECT;
    }

    /**
     * Checks if the connection is an input.  If so, returns the input's view.  Otherwise returns
     * null.
     *
     * @return The {@link InputView} of the {@link Input} on this connection, or null if it doesn't
     * have one.
     */
    @Nullable
    public InputView getInputView() {
        return (getInput() == null) ? null : getInput().getView();
    }

    public boolean isStatementInput() {
        return (getInput() != null && getInput().getType() == Input.TYPE_STATEMENT);
    }

    private void connectInternal(Connection target) {
        mTargetConnection = target;
    }

    private void disconnectInternal() {
        mTargetConnection = null;
    }

    @VisibleForTesting
    public void checkConnection(Connection target) {
        switch (canConnectWithReason(target)) {
            case CAN_CONNECT:
                break;
            case REASON_SELF_CONNECTION:
                throw new IllegalArgumentException("Cannot connect a block to itself.");
            case REASON_WRONG_TYPE:
                throw new IllegalArgumentException("Cannot connect these types.");
            case REASON_MUST_DISCONNECT:
                throw new IllegalStateException(
                        "Must disconnect from current block before connecting to a new one.");
            case REASON_TARGET_NULL:
                throw new IllegalArgumentException("Cannot connect to a null connection/block");
            case REASON_CHECKS_FAILED:
                throw new IllegalArgumentException("Cannot connect, checks do not match.");
            default:
                throw new IllegalArgumentException(
                        "Unknown connection failure, this should never happen!");
        }
    }

    private boolean checksMatch(Connection target) {
        if (mConnectionChecks == null || target.mConnectionChecks == null) {
            return true;
        }
        // The list of checks is expected to be very small (1 or 2 items usually), so the
        // n^2 approach should be fine.
        for (int i = 0; i < mConnectionChecks.length; i++) {
            for (int j = 0; j < target.mConnectionChecks.length; j++) {
                if (TextUtils.equals(mConnectionChecks[i], target.mConnectionChecks[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Connection cloneConnection(Connection conn) {
        if (conn == null) {
            return null;
        }
        return conn.clone();
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CONNECTION_TYPE_PREVIOUS, CONNECTION_TYPE_NEXT, CONNECTION_TYPE_INPUT,
            CONNECTION_TYPE_OUTPUT})
    public @interface ConnectionType {
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CAN_CONNECT, REASON_SELF_CONNECTION, REASON_WRONG_TYPE, REASON_MUST_DISCONNECT,
            REASON_TARGET_NULL, REASON_CHECKS_FAILED})
    public @interface CheckResultType {
    }
}
