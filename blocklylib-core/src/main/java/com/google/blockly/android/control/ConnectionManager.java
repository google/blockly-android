/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.blockly.android.control;

import android.support.annotation.VisibleForTesting;
import android.util.Pair;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.WorkspacePoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Connections.
 */
public class ConnectionManager {
    private static final String TAG = "ConnectionManager";

    private final YSortedList mPreviousConnections = new YSortedList();
    private final YSortedList mNextConnections = new YSortedList();
    private final YSortedList mInputConnections = new YSortedList();
    private final YSortedList mOutputConnections = new YSortedList();

    // If updating this, also update Connection.java's OPPOSITE_TYPES array.
    // The arrays are indexed by connection type codes (conn.getType()).
    private final YSortedList[] matchingLists = new YSortedList[]{
            mPreviousConnections, mNextConnections, mInputConnections, mOutputConnections};
    private final YSortedList[] oppositeLists = new YSortedList[]{
            mNextConnections, mPreviousConnections, mOutputConnections, mInputConnections
    };

    /**
     * Figure out which list the connection belongs in; insert it.
     *
     * @param conn The connection to add.
     */
    public void addConnection(Connection conn) {
        matchingLists[conn.getType()].addConnection(conn);
    }

    /**
     * Remove a connection from the list that handles connections of its type.
     *
     * @param conn The connection to remove.
     */
    public void removeConnection(Connection conn) {
        matchingLists[conn.getType()].removeConnection(conn);
    }

    /**
     * Move the given connector to a specific location and update the relevant list.
     *
     * @param conn The connection to move.
     * @param newLocation The position to move to.
     * @param offset An additional offset, usually the position of the parent view in the workspace
     * view.
     */
    public void moveConnectionTo(Connection conn,
                                 WorkspacePoint newLocation, WorkspacePoint offset) {
        moveConnectionTo(conn, newLocation.x + offset.x, newLocation.y + offset.y);
    }

    /**
     * Clear all the internal state of the manager.
     */
    public void clear() {
        mInputConnections.clear();
        mOutputConnections.clear();
        mPreviousConnections.clear();
        mNextConnections.clear();
    }

    /**
     * Find the closest compatible connection to this connection.
     *
     * @param conn The base connection for the search.
     * @param maxRadius How far out to search for compatible connections.
     *
     * @return The closest compatible connection.
     */
    public Connection closestConnection(Connection conn, double maxRadius) {
        if (conn.isConnected()) {
            // Don't offer to connect when already connected.
            return null;
        }
        YSortedList compatibleList = oppositeLists[conn.getType()];
        return compatibleList.searchForClosest(conn, maxRadius);
    }

    /**
     * Find all compatible connections within the given radius.  This function is used for
     * bumping so type checking does not apply.
     *
     * @param conn The base connection for the search.
     * @param maxRadius How far out to search for compatible connections.
     *
     * @return A list of all nearby compatible connections.
     */
    public void getNeighbors(Connection conn, int maxRadius, List<Connection> result) {
        result.clear();
        YSortedList compatibleList = oppositeLists[conn.getType()];
        compatibleList.getNeighbours(conn, maxRadius, result);
    }

    /**
     * Move the given connector to a specific location and update the relevant list.
     *
     * @param conn The connection to move.
     * @param newX The x location to move to.
     * @param newY The y location to move to.
     */
    private void moveConnectionTo(Connection conn, int newX, int newY) {
        // Avoid list traversals if it's not actually moving.
        if (conn.getPosition().equals(newX, newY)) {
            return;
        }
        if (conn.inDragMode()) {
            conn.setPosition(newX, newY);
        } else {
            removeConnection(conn);
            conn.setPosition(newX, newY);
            addConnection(conn);
        }
    }

    /**
     * Returns the connection that is closest to the given connection.
     *
     * @param base The {@link Connection} to measure from.
     * @param one The first {@link Connection} to check.
     * @param two The second {@link Connection} to check.
     *
     * @return The closer of the two connections.
     */
    private Connection closer(Connection base, Connection one, Connection two) {
        if (one == null) {
            return two;
        }
        if (two == null) {
            return one;
        }

        if (base.distanceFrom(one) < base.distanceFrom(two)) {
            return one;
        }
        return two;
    }

    /**
     * Check if the two connections can be dragged to connect to each other.
     *
     * @param moving The connection being dragged.
     * @param candidate A nearby connection to check.  Must be in the {@link ConnectionManager},
     * and therefore not be mid-drag.
     * @param maxRadius The maximum radius allowed for connections.
     * @param allowShadowParent False if shadows should not be allowed as parents of non-shadow
     *                          blocks, true to skip the shadow parent check.
     *
     * @return True if the connection is allowed, false otherwise.
     */
    @VisibleForTesting
    boolean isConnectionAllowed(Connection moving, Connection candidate, double maxRadius,
            boolean allowShadowParent) {
        if (moving.distanceFrom(candidate) > maxRadius) {
            return false;
        }

        // Type checking
        int canConnect = moving.canConnectWithReason(candidate);
        if (canConnect != Connection.CAN_CONNECT
                && canConnect != Connection.REASON_MUST_DISCONNECT) {
            return false;
        }

        // Don't offer to connect an already connected left (male) value plug to
        // an available right (female) value plug.  Don't offer to connect the
        // bottom of a statement block to one that's already connected.
        if (candidate.getType() == Connection.CONNECTION_TYPE_OUTPUT
                || candidate.getType() == Connection.CONNECTION_TYPE_PREVIOUS) {
            if (candidate.isConnected()) {
                return false;
            }
        }

        if (!allowShadowParent) {
            Block parent;
            Block child;
            if (moving.getType() == Connection.CONNECTION_TYPE_INPUT
                    || moving.getType() == Connection.CONNECTION_TYPE_NEXT) {
                parent = moving.getBlock();
                child = candidate.getBlock();
            } else {
                parent = candidate.getBlock();
                child = moving.getBlock();
            }
            if (parent.isShadow() && !child.isShadow()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Iterate over all of the connections on the block and find the one that is closest to a valid
     * connection on another block.
     *
     * @param block The {@link Block} whose connections to search.
     * @param radiusConnectionWS The maximum distance between viable connections in workspace units.
     * @return A pair of connections, where the first is a connection on {@code block} and the
     *     second is the closest compatible connection.
     */
    public Pair<Connection, Connection> findBestConnection(Block block, int radiusConnectionWS) {
        // Find the connection that is closest to any connection on the block.
        Connection potentialBlockConnection = null;
        Connection potentialCompatibleConnection = null;
        List<Connection> blockConnections = block.getAllConnections();
        Connection curBlockConnection;
        Connection curCompatibleConnection;

        double maxSearchRadius = radiusConnectionWS;

        for (int i = 0; i < blockConnections.size(); i++) {
            curBlockConnection = blockConnections.get(i);
            curCompatibleConnection =
                    closestConnection(curBlockConnection, maxSearchRadius);
            if (curCompatibleConnection != null) {
                potentialBlockConnection = curBlockConnection;
                potentialCompatibleConnection = curCompatibleConnection;
                maxSearchRadius =
                        potentialBlockConnection.distanceFrom(potentialCompatibleConnection);
            }
        }
        if (potentialBlockConnection == null) {
            return null;
        }
        return new Pair<>(potentialBlockConnection, potentialCompatibleConnection);
    }

    @VisibleForTesting
    YSortedList getConnections(int connectionType) {
        return matchingLists[connectionType];
    }

    /**
     * List of connections ordered by y position.  This is optimized
     * for quickly finding the nearest connection when dragging a block around.
     * Connections are not ordered by their x position and multiple connections may be at the same
     * y position.
     */
    @VisibleForTesting
    class YSortedList {
        private final List<Connection> mConnections = new ArrayList<>();

        /**
         * Insert the given connection into this list.
         *
         * @param conn The connection to insert.
         */
        public void addConnection(Connection conn) {
            int position = findPositionForConnection(conn);
            if (position < mConnections.size() && conn == mConnections.get(position)) {
                throw new IllegalArgumentException("Already added.");
            }
            mConnections.add(position, conn);
        }

        /**
         * Remove the given connection from this list.
         *
         * @param conn The connection to remove.
         */
        public void removeConnection(Connection conn) {
            int removalIndex = findConnection(conn);
            if (removalIndex != -1) {
                mConnections.remove(removalIndex);
            }
        }

        public void clear() {
            mConnections.clear();
        }

        private boolean isInYRange(int index, int baseY, double maxRadius) {
            int curY = mConnections.get(index).getPosition().y;
            return (Math.abs(curY - baseY) <= maxRadius);
        }

        /**
         * Find the given connection in the given list.
         * Starts by doing a binary search to find the approximate location, then linearly searches
         * nearby for the exact connection.
         *
         * @param conn The connection to find.
         *
         * @return The index of the connection, or -1 if the connection was not found.
         */
        @VisibleForTesting
        int findConnection(Connection conn) {
            if (mConnections.isEmpty()) {
                return -1;
            }
            // Should have the right y position.
            int bestGuess = findPositionForConnection(conn);
            if (bestGuess >= mConnections.size()) {
                // Not in list.
                return -1;
            }

            int yPos = conn.getPosition().y;
            // Walk forward and back on the y axis looking for the connection.
            // When found, splice it out of the array.
            int pointerMin = bestGuess;
            int pointerMax = bestGuess + 1;
            while (pointerMin >= 0 && mConnections.get(pointerMin).getPosition().y == yPos) {
                if (mConnections.get(pointerMin) == conn) {
                    return pointerMin;
                }
                pointerMin--;
            }
            while (pointerMax < mConnections.size() &&
                    mConnections.get(pointerMax).getPosition().y == yPos) {
                if (mConnections.get(pointerMax) == conn) {
                    return pointerMax;
                }
                pointerMax++;
            }
            return -1;
        }

        /**
         * Finds a candidate position for inserting this connection into the given list.
         * This will be in the correct y order but makes no guarantees about ordering in the x axis.
         *
         * @param conn The connection to insert.
         *
         * @return The candidate index.
         */
        @VisibleForTesting
        int findPositionForConnection(Connection conn) {
            if (mConnections.isEmpty()) {
                return 0;
            }
            int pointerMin = 0;
            int pointerMax = mConnections.size();
            int yPos = conn.getPosition().y;
            while (pointerMin < pointerMax) {
                int pointerMid = (pointerMin + pointerMax) / 2;
                int pointerY = mConnections.get(pointerMid).getPosition().y;
                if (pointerY < yPos) {
                    pointerMin = pointerMid + 1;
                } else if (pointerY > yPos) {
                    pointerMax = pointerMid;
                } else {
                    pointerMin = pointerMid;
                    break;
                }
            }
            return pointerMin;
        }

        @VisibleForTesting
        Connection searchForClosest(Connection conn, double maxRadius) {
            // Don't bother.
            if (mConnections.isEmpty()) {
                return null;
            }

            int baseY = conn.getPosition().y;
            // findPositionForConnection finds an index for insertion, which is always after any
            // block with the same y index.  We want to search both forward and back, so search
            // on both sides of the index.
            int closestIndex = findPositionForConnection(conn);

            Connection bestConnection = null;
            double bestRadius = maxRadius;

            // Walk forward and back on the y axis looking for the closest x,y point.
            int pointerMin = closestIndex - 1;
            while (pointerMin >= 0 && isInYRange(pointerMin, baseY, maxRadius)) {
                Connection temp = mConnections.get(pointerMin);
                if (isConnectionAllowed(conn, temp, bestRadius, false)) {
                    bestConnection = temp;
                    bestRadius = temp.distanceFrom(conn);
                }
                pointerMin--;
            }

            int pointerMax = closestIndex;
            while (pointerMax < mConnections.size() && isInYRange(pointerMax, baseY, maxRadius)) {
                Connection temp = mConnections.get(pointerMax);
                if (isConnectionAllowed(conn, temp, bestRadius, false)) {
                    bestConnection = temp;
                    bestRadius = temp.distanceFrom(conn);
                }
                pointerMax++;
            }

            return bestConnection;
        }

        @VisibleForTesting
        void getNeighbours(Connection conn, int maxRadius, List<Connection> neighbours) {
            // Don't bother.
            if (mConnections.isEmpty()) {
                return;
            }

            int baseY = conn.getPosition().y;
            // findPositionForConnection finds an index for insertion, which is always after any
            // block with the same y index.  We want to search both forward and back, so search
            // on both sides of the index.
            int closestIndex = findPositionForConnection(conn);

            // Walk forward and back on the y axis looking for the closest x,y point.
            // If both connections are connected, that's probably fine.  But if
            // either one of them is unconnected, then there could be confusion.
            int pointerMin = closestIndex - 1;
            while (pointerMin >= 0 && isInYRange(pointerMin, baseY, maxRadius)) {
                Connection temp = mConnections.get(pointerMin);
                if ((!conn.isConnected() || !temp.isConnected())
                        && isConnectionAllowed(conn, temp, maxRadius, true)) {
                    neighbours.add(temp);
                }
                pointerMin--;
            }

            int pointerMax = closestIndex;
            while (pointerMax < mConnections.size() && isInYRange(pointerMax, baseY, maxRadius)) {
                Connection temp = mConnections.get(pointerMax);
                if ((!conn.isConnected() || !temp.isConnected())
                        && isConnectionAllowed(conn, temp, maxRadius, true)) {
                    neighbours.add(temp);
                }
                pointerMax++;
            }
            return;
        }

        @VisibleForTesting
        boolean isEmpty() {
            return mConnections.isEmpty();
        }

        @VisibleForTesting
        int size() {
            return mConnections.size();
        }

        @VisibleForTesting
        boolean contains(Connection conn) {
            return findConnection(conn) != -1;
        }

        @VisibleForTesting
        Connection get(int i) {
            return mConnections.get(i);
        }
    }
}
