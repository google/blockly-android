package com.google.blockly.model;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Describes a connection on a Block. This can be a previous/next connection, an output, or
 * the connection on an {@link Input}.
 */
public class Connection {
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

    private static final int[] OPPOSITE_TYPES = new int[]{
            CONNECTION_TYPE_NEXT, // PREVIOUS -> NEXT
            CONNECTION_TYPE_PREVIOUS, // NEXT -> PREVIOUS
            CONNECTION_TYPE_OUTPUT, // INPUT -> OUTPUT
            CONNECTION_TYPE_INPUT // OUTPUT -> INPUT
    };

    private static final int CAN_CONNECT = 0;
    private static final int REASON_SELF_CONNECTION = 1;
    private static final int REASON_WRONG_TYPE = 2;
    private static final int REASON_MUST_DISCONNECT = 3;
    private static final int REASON_TARGET_NULL = 4;
    private static final int REASON_CHECKS_FAILED = 5;


    private final int mConnectionType;
    private Block mBlock;
    private Input mInput;
    private String[] mConnectionChecks;
    private Connection mTargetConnection;

    public Connection(int type, String[] checks) {
        mConnectionType = type;
        mConnectionChecks = checks;
    }

    /**
     * Check if this can be connected to the target connection.
     *
     * @param target The connection to check.
     * @return True if the target can be connected, false otherwise.
     */
    public boolean canConnect(Connection target) {
        return canConnectWithReason(target) == CAN_CONNECT;
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

    /**
     * Sets the block that this connection is part of.
     */
    public void setBlock(Block block) {
        mBlock = block;
    }

    /**
     * Sets the input this connection is part of.
     */
    public void setInput(Input input) {
        mInput = input;
    }

    /**
     * @return The {@link Block} this is connected to or null if it is not connected.
     */
    public Block getTargetBlock() {
        return mTargetConnection == null ? null : mTargetConnection.getBlock();
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
     *     <li>{@link #CONNECTION_TYPE_PREVIOUS}</li>
     *     <li>{@link #CONNECTION_TYPE_NEXT}</li>
     *     <li>{@link #CONNECTION_TYPE_INPUT}</li>
     *     <li>{@link #CONNECTION_TYPE_OUTPUT}</li>
     * </ul>
     * @return The type of this connection.
     */
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
     * @return The Input for this connection or null.
     */
    public Input getInput() {
        return mInput;
    }

    private void connectInternal(Connection target) {
        mTargetConnection = target;
    }

    private void disconnectInternal() {
        mTargetConnection = null;
    }

    private int canConnectWithReason(Connection target) {
        if (target == null) {
            return REASON_TARGET_NULL;
        }
        if (target.getBlock() == getBlock()) {
            return REASON_SELF_CONNECTION;
        }
        if (target.getType() != OPPOSITE_TYPES[mConnectionType]) {
            return REASON_WRONG_TYPE;
        }
        if (mTargetConnection != null) {
            return REASON_MUST_DISCONNECT;
        }
        if (!checksMatch(target)) {
            return REASON_CHECKS_FAILED;
        }
        return CAN_CONNECT;
    }

    private void checkConnection(Connection target) {
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
                throw new IllegalArgumentException("Cannot connect to a null connection");
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
}
