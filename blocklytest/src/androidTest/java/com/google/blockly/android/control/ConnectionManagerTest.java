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

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Input;
import com.google.blockly.model.WorkspacePoint;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link ConnectionManager}
 */
public class ConnectionManagerTest {
    private ConnectionManager manager;

    @Before
    public void setUp() {
        manager = new ConnectionManager();
    }

    @Test
    public void testAdd() {
        Connection conn = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        manager.addConnection(conn);
        assertThat(manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).contains(conn))
            .isTrue();

        conn = new Connection(Connection.CONNECTION_TYPE_NEXT, null);
        manager.addConnection(conn);
        assertThat(manager.getConnections(Connection.CONNECTION_TYPE_NEXT).contains(conn))
            .isTrue();

        conn = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        manager.addConnection(conn);
        assertThat(manager.getConnections(Connection.CONNECTION_TYPE_INPUT).contains(conn))
            .isTrue();

        conn = new Connection(Connection.CONNECTION_TYPE_OUTPUT, null);
        manager.addConnection(conn);
        assertThat(manager.getConnections(Connection.CONNECTION_TYPE_OUTPUT).contains(conn))
            .isTrue();
    }

    @Test
    public void testMoveTo() {
        int offsetX = 10;
        int offsetY = -10;
        WorkspacePoint offset = new WorkspacePoint(offsetX, offsetY);
        Connection conn = createConnection(0, 0, Connection.CONNECTION_TYPE_PREVIOUS, false);
        manager.addConnection(conn);
        // Move to this position + the given offset.
        int moveX = 15;
        int moveY = 20;
        manager.moveConnectionTo(conn, new WorkspacePoint(moveX, moveY), offset);
        assertThat(conn.getPosition().x).isEqualTo(moveX + offsetX);
        assertThat(conn.getPosition().y).isEqualTo(moveY + offsetY);
        // Connection should still be in the list
        assertThat(manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).contains(conn))
            .isTrue();

        manager.removeConnection(conn);
        conn.setDragMode(true);
        // Moving a connection while being dragged should update the connection itself but not
        // put it back into the connection manager.
        moveX = 10;
        moveY = 100;
        manager.moveConnectionTo(conn, new WorkspacePoint(moveX, moveY), offset);
        assertThat(manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).contains(conn))
            .isFalse();
        assertThat(conn.getPosition().x).isEqualTo(moveX + offsetX);
        assertThat(conn.getPosition().y).isEqualTo(moveY + offsetY);
    }

    @Test
    public void testIsConnectionAllowed() {
        // Two connections of opposite types near each other
        Connection one = createConnection(5 /* x */, 10 /* y */, Connection.CONNECTION_TYPE_INPUT,
                false);
        Connection two = createConnection(10 /* x */, 15 /* y */, Connection.CONNECTION_TYPE_OUTPUT,
                false);

        assertThat(manager.isConnectionAllowed(one, two, 20.0, false)).isTrue();
        // Move connections farther apart
        two.setPosition(100, 100);
        assertThat(manager.isConnectionAllowed(one, two, 20.0, false)).isFalse();

        // Don't offer to connect an already connected left (male) value plug to
        // an available right (female) value plug.
        Connection three = createConnection(0, 0, Connection.CONNECTION_TYPE_OUTPUT, false);
        assertThat(manager.isConnectionAllowed(one, three, 20.0, false)).isTrue();
        Connection four = createConnection(0, 0, Connection.CONNECTION_TYPE_INPUT, false);
        three.connect(four);
        assertThat(manager.isConnectionAllowed(one, three, 20.0, false)).isFalse();

        // Don't connect two connections on the same block
        two.setBlock(one.getBlock());
        assertThat(manager.isConnectionAllowed(one, two, 1000.0, false)).isFalse();

        Connection five = createConnection(0, 0, Connection.CONNECTION_TYPE_INPUT, true);
        Connection six = createConnection(0, 0, Connection.CONNECTION_TYPE_OUTPUT, true);
        Connection seven = createConnection(0, 0, Connection.CONNECTION_TYPE_OUTPUT, false);

        // Verify that shadows can be parents of other shadows
        assertThat(manager.isConnectionAllowed(five, six, 1000.0, false)).isTrue();
        // But not parents of non-shadows
        assertThat(manager.isConnectionAllowed(five, seven, 1000.0, false)).isFalse();
        // Unless shadow parents are explicitly allowed
        assertThat(manager.isConnectionAllowed(five, seven, 1000.0, true)).isTrue();
    }

    @Test
    public void testIsConnectionAllowedNext() {
        Connection one = createConnection(0, 0, Connection.CONNECTION_TYPE_NEXT, false);
        one.setInput(new Input.InputValue("test input", "" /* align */, null /* checks */));

        Connection two = createConnection(0, 0, Connection.CONNECTION_TYPE_NEXT, false);
        two.setInput(new Input.InputValue("test input", "" /* align */, null /* checks */));

        // Don't offer to connect the bottom of a statement block to one that's already connected.
        Connection three = createConnection(0, 0, Connection.CONNECTION_TYPE_PREVIOUS, false);
        assertThat(manager.isConnectionAllowed(one, three, 20.0, false)).isTrue();
        three.connect(two);
        assertThat(manager.isConnectionAllowed(one, three, 20.0, false)).isFalse();

        Connection four = createConnection(0, 0, Connection.CONNECTION_TYPE_NEXT, true);
        Connection five = createConnection(0, 0, Connection.CONNECTION_TYPE_PREVIOUS, true);
        Connection six = createConnection(0, 0, Connection.CONNECTION_TYPE_PREVIOUS, false);

        // Verify that shadows can be parents of other shadows
        assertThat(manager.isConnectionAllowed(four, five, 1000.0, false)).isTrue();
        // But not parents of non-shadows
        assertThat(manager.isConnectionAllowed(four, six, 1000.0, false)).isFalse();
        // Unless shadow parents are explicitly allowed
        assertThat(manager.isConnectionAllowed(four, six, 1000.0, true)).isTrue();
    }

    // Test YSortedList
    @Test
    public void testFindPosition() {
        ConnectionManager.YSortedList list =
                manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS);
        list.addConnection(createConnection(0, 0, Connection.CONNECTION_TYPE_PREVIOUS, false));
        list.addConnection(createConnection(0, 1, Connection.CONNECTION_TYPE_PREVIOUS, false));
        list.addConnection(createConnection(0, 2, Connection.CONNECTION_TYPE_PREVIOUS, false));
        list.addConnection(createConnection(0, 4, Connection.CONNECTION_TYPE_PREVIOUS, false));
        list.addConnection(createConnection(0, 5, Connection.CONNECTION_TYPE_PREVIOUS, false));

        assertThat(list.size()).isEqualTo(5);
        Connection conn = createConnection(0, 3, Connection.CONNECTION_TYPE_PREVIOUS, false);
        assertThat(list.findPositionForConnection(conn)).isEqualTo(3);
    }

    // Test YSortedList
    @Test
    public void testFind() {
        ConnectionManager.YSortedList previous = manager.getConnections(
                Connection.CONNECTION_TYPE_PREVIOUS);
        for (int i = 0; i < 10; i++) {
            previous.addConnection(createConnection(i, 0, Connection.CONNECTION_TYPE_PREVIOUS,
                    false));
            previous.addConnection(createConnection(0, i, Connection.CONNECTION_TYPE_PREVIOUS,
                    false));
        }

        Connection conn = createConnection(3, 3, Connection.CONNECTION_TYPE_PREVIOUS, false);
        previous.addConnection(conn);
        assertThat(previous.get(previous.findConnection(conn))).isEqualTo(conn);

        conn = createConnection(3, 3, Connection.CONNECTION_TYPE_PREVIOUS, false);
        assertThat(previous.findConnection(conn)).isEqualTo(-1);
    }

    @Test
    public void testOrdered() {
        ConnectionManager.YSortedList list = manager.getConnections(
                Connection.CONNECTION_TYPE_PREVIOUS);
        for (int i = 0; i < 10; i++) {
            list.addConnection(createConnection(0, 9 - i, Connection.CONNECTION_TYPE_PREVIOUS,
                    false));
        }

        for (int i = 0; i < 10; i++) {
            assertThat(list.get(i).getPosition().y).isEqualTo(i);
        }

        // quasi-random
        int[] xCoords = {-29, -47, -77, 2, 43, 34, -59, -52, -90, -36, -91, 38, 87, -20, 60, 4, -57,
                65, -37, -81, 57, 58, -96, 1, 67, -79, 34, 93, -90, -99, -62, 4, 11, -36, -51, -72,
                3, -50, -24, -45, -92, -38, 37, 24, -47, -73, 79, -20, 99, 43, -10, -87, 19, 35,
                -62, -36, 49, 86, -24, -47, -89, 33, -44, 25, -73, -91, 85, 6, 0, 89, -94, 36, -35,
                84, -9, 96, -21, 52, 10, -95, 7, -67, -70, 62, 9, -40, -95, -9, -94, 55, 57, -96,
                55, 8, -48, -57, -87, 81, 23, 65};
        int[] yCoords = {-81, 82, 5, 47, 30, 57, -12, 28, 38, 92, -25, -20, 23, -51, 73, -90, 8, 28,
                -51, -15, 81, -60, -6, -16, 77, -62, -42, -24, 35, 95, -46, -7, 61, -16, 14, 91, 57,
                -38, 27, -39, 92, 47, -98, 11, -33, -72, 64, 38, -64, -88, -35, -59, -76, -94, 45,
                -25, -100, -95, 63, -97, 45, 98, 99, 34, 27, 52, -18, -45, 66, -32, -38, 70, -73,
                -23, 5, -2, -13, -9, 48, 74, -97, -11, 35, -79, -16, -77, 83, -57, -53, 35, -44,
                100, -27, -15, 5, 39, 33, -19, -20, -95};
        for (int i = 0; i < xCoords.length; i++) {
            list.addConnection(createConnection(xCoords[i], yCoords[i],
                Connection.CONNECTION_TYPE_PREVIOUS, false));
        }

        for (int i = 1; i < xCoords.length; i++) {
            assertThat(list.get(i).getPosition().y >= list.get(i - 1).getPosition().y).isTrue();
        }
    }

    // Test YSortedList
    @Test
    public void testSearchForClosest() {
        ConnectionManager.YSortedList list =
                manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS);

        // search an empty list
        assertThat(searchList(list, 10 /* x */, 10 /* y */, 100 /* radius */)).isNull();

        list.addConnection(createConnection(100, 0, Connection.CONNECTION_TYPE_PREVIOUS, false));
        assertThat(searchList(list, 0, 0, 5)).isNull();
        list.clear();

        for (int i = 0; i < 10; i++) {
            list.addConnection(createConnection(0, i, Connection.CONNECTION_TYPE_PREVIOUS, false));
        }

        // should be at 0, 9
        Connection last = list.get(list.size() - 1);
        // correct connection is last in list; many connections in radius
        assertThat(searchList(list, 0, 10, 15)).isEqualTo(last);
        // Nothing nearby.
        assertThat(searchList(list, 100, 100, 3)).isNull();
        // first in list, exact match
        assertThat(searchList(list, 0, 0, 0)).isEqualTo(list.get(0));

        list.addConnection(createConnection(6, 6, Connection.CONNECTION_TYPE_PREVIOUS, false));
        list.addConnection(createConnection(5, 5, Connection.CONNECTION_TYPE_PREVIOUS, false));

        Connection result = searchList(list, 4, 6, 3);
        assertThat(result.getPosition().x).isEqualTo(5);
        assertThat(result.getPosition().y).isEqualTo(5);
    }

    @Test
    public void testGetNeighbours() {

        ConnectionManager.YSortedList list =
                manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS);

        // Search an empty list
        assertThat(getNeighbourHelper(list, 10 /* x */, 10 /* y */, 100 /* radius */).isEmpty())
            .isTrue();

        // Make a list
        for (int i = 0; i < 10; i++) {
            list.addConnection(createConnection(0, i, Connection.CONNECTION_TYPE_PREVIOUS, false));
        }

        // Test block belongs at beginning
        List<Connection> result = getNeighbourHelper(list, 0, 0, 4);
        assertThat(result.size()).isEqualTo(5);
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.contains(list.get(i))).isTrue();
        }

        // Test block belongs at middle
        result = getNeighbourHelper(list, 0, 4, 2);
        assertThat(result.size()).isEqualTo(5);
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.contains(list.get(i + 2))).isTrue();
        }

        // Test block belongs at end
        result = getNeighbourHelper(list, 0, 9, 4);
        assertThat(result.size()).isEqualTo(5);
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.contains(list.get(i + 5))).isTrue();
        }

        // Test block has no neighbours due to being out of range in the x direction
        result = getNeighbourHelper(list, 10, 9, 4);
        assertThat(result.isEmpty()).isTrue();

        // Test block has no neighbours due to being out of range in the y direction
        result = getNeighbourHelper(list, 0, 19, 4);
        assertThat(result.isEmpty()).isTrue();

        // Test block has no neighbours due to being out of range diagonally
        result = getNeighbourHelper(list, -2, -2, 2);
        assertThat(result.isEmpty()).isTrue();
    }

    private List<Connection> getNeighbourHelper(ConnectionManager.YSortedList list, int x, int y,
                                                int radius) {
        List<Connection> result = new ArrayList<>();
        list.getNeighbours(
            createConnection(x, y, Connection.CONNECTION_TYPE_NEXT, false), radius, result);
        return result;
    }

    // Helper
    private Connection searchList(ConnectionManager.YSortedList list, int x, int y, int radius) {
        return list.searchForClosest(createConnection(x, y, Connection.CONNECTION_TYPE_NEXT, false),
                radius);
    }

    private Connection createConnection(int x, int y, int type, boolean shadow) {
        Connection conn = new Connection(type, null);
        conn.setPosition(x, y);
        conn.setBlock(new Block.Builder("test").setShadow(shadow).build());
        return conn;
    }
}
