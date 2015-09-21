/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.control;

import android.support.annotation.VisibleForTesting;

import com.google.blockly.model.Connection;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Connections.
 */
public class ConnectionManager {
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

    public void removeConnection(Connection conn) {
        // TODO(fenichel): Remove block statistics as well.
        matchingLists[conn.getType()].removeConnection(conn);
    }

    public void moveConnection(Connection conn, int dx, int dy) {
        removeConnection(conn);
        conn.setPosition(conn.getPosition().x + dx, conn.getPosition().y + dy);
        addConnection(conn);
    }

    public void clear() {
        mInputConnections.clear();
        mOutputConnections.clear();
        mPreviousConnections.clear();
        mNextConnections.clear();
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
            mConnections.add(findPositionForConnection(conn), conn);
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

        /**
         * Find the given connection in the given list.
         * Starts by doing a binary search to find the approximate location, then linearly searches
         * nearby for the exact connection.
         *
         * @param conn The connection to find.
         * @return The index of the connection, or -1 if the connection was not found.
         */
        @VisibleForTesting
        int findConnection(Connection conn) {
            // Should have the right y position.
            int bestGuess = findPositionForConnection(conn);
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
         * @return The candidate index.
         */
        @VisibleForTesting
        int findPositionForConnection(Connection conn) {
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
