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

import android.test.AndroidTestCase;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Input;
import com.google.blockly.ui.ViewPoint;

/**
 * Tests for {@link ConnectionManager}
 */
public class ConnectionManagerTest extends AndroidTestCase {
    private ConnectionManager manager;

    @Override
    public void setUp() {
        manager = new ConnectionManager();
    }

    public void testAdd() {
        Connection conn = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        manager.addConnection(conn);
        assertTrue(manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).contains(conn));

        conn = new Connection(Connection.CONNECTION_TYPE_NEXT, null);
        manager.addConnection(conn);
        assertTrue(manager.getConnections(Connection.CONNECTION_TYPE_NEXT).contains(conn));

        conn = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        manager.addConnection(conn);
        assertTrue(manager.getConnections(Connection.CONNECTION_TYPE_INPUT).contains(conn));

        conn = new Connection(Connection.CONNECTION_TYPE_OUTPUT, null);
        manager.addConnection(conn);
        assertTrue(manager.getConnections(Connection.CONNECTION_TYPE_OUTPUT).contains(conn));
    }

    public void testMoveTo() {
        int offsetX = 10;
        int offsetY = -10;
        ViewPoint offset = new ViewPoint(offsetX, offsetY);
        Connection conn = createConnection(0, 0, Connection.CONNECTION_TYPE_PREVIOUS);
        manager.addConnection(conn);
        // Move to this position + the given offset.
        int moveX = 15;
        int moveY = 20;
        manager.moveConnectionTo(conn, new ViewPoint(moveX, moveY), offset);
        assertEquals(moveX + offsetX, conn.getPosition().x);
        assertEquals(moveY + offsetY, conn.getPosition().y);
        // Connection should still be in the list
        assertTrue(manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).contains(conn));

        manager.removeConnection(conn);
        conn.setDragMode(true);
        // Moving a connection while being dragged should update the connection itself but not
        // put it back into the connection manager.
        moveX = 10;
        moveY = 100;
        manager.moveConnectionTo(conn, new ViewPoint(moveX, moveY), offset);
        assertFalse(manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).contains(conn));
        assertEquals(moveX + offsetX, conn.getPosition().x);
        assertEquals(moveY + offsetY, conn.getPosition().y);
    }

    public void testIsConnectionAllowed() {
        // Two connections of opposite types near each other
        Connection one = createConnection(5 /* x */, 10 /* y */, Connection.CONNECTION_TYPE_INPUT);
        Connection two = createConnection(10 /* x */, 15 /* y */, Connection.CONNECTION_TYPE_OUTPUT);

        assertTrue(manager.isConnectionAllowed(one, two, 20.0));
        // Move connections farther apart
        two.setPosition(100, 100);
        assertFalse(manager.isConnectionAllowed(one, two, 20.0));

        // Don't offer to connect an already connected left (male) value plug to
        // an available right (female) value plug.
        Connection three = createConnection(0, 0, Connection.CONNECTION_TYPE_OUTPUT);
        assertTrue(manager.isConnectionAllowed(one, three, 20.0));
        Connection four = createConnection(0, 0, Connection.CONNECTION_TYPE_INPUT);
        three.connect(four);
        assertFalse(manager.isConnectionAllowed(one, three, 20.0));

        // Don't connect two connections on the same block
        two.setBlock(one.getBlock());
        assertFalse(manager.isConnectionAllowed(one, two, 1000.0));
    }

    public void testIsConnectionAllowedNext() {
        Connection one = createConnection(0, 0, Connection.CONNECTION_TYPE_NEXT);
        one.setInput(new Input.InputValue("test input", "" /* align */, null /* checks */));

        Connection two = createConnection(0, 0, Connection.CONNECTION_TYPE_NEXT);
        two.setInput(new Input.InputValue("test input", "" /* align */, null /* checks */));

        // Don't offer to connect the bottom of a statement block to one that's already connected.
        Connection three = createConnection(0, 0, Connection.CONNECTION_TYPE_PREVIOUS);
        assertTrue(manager.isConnectionAllowed(one, three, 20.0));
        three.connect(two);
        assertFalse(manager.isConnectionAllowed(one, three, 20.0));
    }

    public void testIsConnectionAllowedNest() {
        Input input = new Input.InputValue("input", null, null);
        Block root = new Block.Builder("test")
                .setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null))
                .addInput(input)
                .build();
        Block child = new Block.Builder("test")
                .setPrevious(new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null))
                .setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null))
                .addInput(input)
                .build();
        root.getNextConnection().connect(child.getPreviousConnection());
        Block grandchild = new Block.Builder("test")
                .setPrevious(new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null))
                .setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null))
                .build();
        child.getNextConnection().connect(grandchild.getPreviousConnection());

        // Don't let blocks try to connect to themselves or ones they nest.
        assertFalse(manager.isConnectionAllowed(root.getNextConnection(),
                grandchild.getPreviousConnection(), 100.0 /* maxRadius */));

        // But blocks can be dragged to connect to an ancestor block.
        assertTrue(manager.isConnectionAllowed(grandchild.getPreviousConnection(),
                root.getNextConnection(), 100.0));

        Block withOutput = new Block.Builder("withOutput")
                .setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null))
                .build();

        withOutput.getOutputConnection().connect(child.getInputByName("input").getConnection());

        // Don't let blocks try to connect to themselves or ones they nest.
        assertFalse(manager.isConnectionAllowed(root.getInputByName("input").getConnection(),
                withOutput.getOutputConnection(), 100.0));
        // But blocks can be dragged to connect to an ancestor block.
        assertTrue(manager.isConnectionAllowed(withOutput.getOutputConnection(),
                root.getInputByName("input").getConnection(), 100.0));
    }

    // Test YSortedList
    public void testFindPosition() {
        ConnectionManager.YSortedList list =
                manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS);
        list.addConnection(createConnection(0, 0, Connection.CONNECTION_TYPE_PREVIOUS));
        list.addConnection(createConnection(0, 1, Connection.CONNECTION_TYPE_PREVIOUS));
        list.addConnection(createConnection(0, 2, Connection.CONNECTION_TYPE_PREVIOUS));
        list.addConnection(createConnection(0, 4, Connection.CONNECTION_TYPE_PREVIOUS));
        list.addConnection(createConnection(0, 5, Connection.CONNECTION_TYPE_PREVIOUS));

        assertEquals(5, list.size());
        Connection conn = createConnection(0, 3, Connection.CONNECTION_TYPE_PREVIOUS);
        assertEquals(3, list.findPositionForConnection(conn));
    }

    // Test YSortedList
    public void testFind() {
        ConnectionManager.YSortedList previous = manager.getConnections(
                Connection.CONNECTION_TYPE_PREVIOUS);
        for (int i = 0; i < 10; i++) {
            previous.addConnection(createConnection(i, 0, Connection.CONNECTION_TYPE_PREVIOUS));
            previous.addConnection(createConnection(0, i, Connection.CONNECTION_TYPE_PREVIOUS));
        }

        Connection conn = createConnection(3, 3, Connection.CONNECTION_TYPE_PREVIOUS);
        previous.addConnection(conn);
        assertEquals(conn, previous.get(previous.findConnection(conn)));

        conn = createConnection(3, 3, Connection.CONNECTION_TYPE_PREVIOUS);
        assertEquals(-1, previous.findConnection(conn));
    }

    public void testOrdered() {
        ConnectionManager.YSortedList list = manager.getConnections(
                Connection.CONNECTION_TYPE_PREVIOUS);
        for (int i = 0; i < 10; i++) {
            list.addConnection(createConnection(0, 9 - i, Connection.CONNECTION_TYPE_PREVIOUS));
        }

        for (int i = 0; i < 10; i++){
            assertEquals(i, list.get(i).getPosition().y);
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
            list.addConnection(createConnection(xCoords[i], yCoords[i], Connection.CONNECTION_TYPE_PREVIOUS));
        }

        for (int i = 1; i < xCoords.length; i++) {
            assertTrue(list.get(i).getPosition().y >= list.get(i - 1).getPosition().y);
        }
    }

    // Test YSortedList
    public void testSearchForClosest() {
        ConnectionManager.YSortedList list =
                manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS);

        // search an empty list
        assertEquals(null, searchList(list, 10 /* x */, 10 /* y */, 100 /* radius */));

        list.addConnection(createConnection(100, 0, Connection.CONNECTION_TYPE_PREVIOUS));
        assertEquals(null, searchList(list, 0, 0, 5));
        list.clear();

        for (int i = 0; i < 10; i++) {
            list.addConnection(createConnection(0, i, Connection.CONNECTION_TYPE_PREVIOUS));
        }

        // should be at 0, 9
        Connection last = list.get(list.size() - 1);
        // correct connection is last in list; many connections in radius
        assertEquals(last, searchList(list, 0, 10, 15));
        // Nothing nearby.
        assertEquals(null, searchList(list, 100, 100, 3));
        // first in list, exact match
        assertEquals(list.get(0), searchList(list, 0, 0, 0));

        list.addConnection(createConnection(6, 6, Connection.CONNECTION_TYPE_PREVIOUS));
        list.addConnection(createConnection(5, 5, Connection.CONNECTION_TYPE_PREVIOUS));

        Connection result = searchList(list, 4, 6, 3);
        assertEquals(5, result.getPosition().x);
        assertEquals(5, result.getPosition().y);
    }

    private Connection searchList(ConnectionManager.YSortedList list, int x, int y, int radius) {
        return list.searchForClosest(createConnection(x, y, Connection.CONNECTION_TYPE_NEXT), radius);
    }

    private Connection createConnection(int x, int y, int type) {
        Connection conn = new Connection(type, null);
        conn.setPosition(x, y);
        conn.setBlock(new Block.Builder("test").build());
        return conn;
    }
}
