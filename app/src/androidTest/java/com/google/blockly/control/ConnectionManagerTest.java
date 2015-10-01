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

import com.google.blockly.model.Connection;
import com.google.blockly.ui.ViewPoint;

import java.util.Random;

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
        Connection conn = createConnection(0, 0);
        manager.addConnection(conn);
        manager.moveConnectionTo(conn, new ViewPoint(15, 20));
        assertEquals(15, conn.getPosition().x);
        assertEquals(20, conn.getPosition().y);
        assertTrue(manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).contains(conn));

        manager.removeConnection(conn);
        conn.setDragMode(true);
        manager.moveConnectionTo(conn, new ViewPoint(10, 100));
        assertFalse(manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).contains(conn));
        assertEquals(10, conn.getPosition().x);
        assertEquals(100, conn.getPosition().y);
    }

    // Test YSortedList
    public void testFindPosition() {
        ConnectionManager.YSortedList list =
                manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS);
        list.addConnection(createConnection(0, 0));
        list.addConnection(createConnection(0, 1));
        list.addConnection(createConnection(0, 2));
        list.addConnection(createConnection(0, 4));
        list.addConnection(createConnection(0, 5));

        assertEquals(5, list.size());
        Connection conn = createConnection(0, 3);
        assertEquals(3, list.findPositionForConnection(conn));
    }

    // Test YSortedList
    public void testFind() {
        ConnectionManager.YSortedList previous = manager.getConnections(
                Connection.CONNECTION_TYPE_PREVIOUS);
        for (int i = 0; i < 10; i++) {
            previous.addConnection(createConnection(i, 0));
            previous.addConnection(createConnection(0, i));
        }

        Connection conn = createConnection(3, 3);
        previous.addConnection(conn);
        assertEquals(conn, previous.get(previous.findConnection(conn)));

        conn = createConnection(3, 3);
        assertEquals(-1, previous.findConnection(conn));
    }

    public void testOrdered() {
        ConnectionManager.YSortedList list = manager.getConnections(
                Connection.CONNECTION_TYPE_PREVIOUS);
        for (int i = 0; i < 10; i++) {
            list.addConnection(createConnection(0, 9 - i));
        }

        for (int i = 0; i < 10; i++){
            assertEquals(i, list.get(i).getPosition().y);
        }

        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            list.addConnection(createConnection(rand.nextInt(100), rand.nextInt(100)));
        }

        int lastY = list.get(0).getPosition().y;
        for (int i = 1; i < 100; i++) {
            assertTrue(list.get(i).getPosition().y >= lastY);
        }
    }

    // Test YSortedList
    public void testSearchForClosest() {
        ConnectionManager.YSortedList list =
                manager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS);

        // search an empty list
        assertEquals(null, searchList(list, 10 /* x */, 10 /* y */, 100 /* radius */));

        for (int i = 0; i < 10; i++) {
            list.addConnection(createConnection(0, i));
        }

        // should be at 0, 9
        Connection last = list.get(list.size() - 1);
        // correct connection is last in list; many connections in radius
        assertEquals(last, searchList(list, 0, 10, 15));
        // Nothing nearby.
        assertEquals(null, searchList(list, 100, 100, 3));
        // first in list, exact match
        assertEquals(list.get(0), searchList(list, 0, 0, 0));

        list.addConnection(createConnection(6, 6));
        list.addConnection(createConnection(5, 5));

        Connection result = searchList(list, 4, 6, 3);
        assertEquals(5, result.getPosition().x);
        assertEquals(5, result.getPosition().y);
    }

    private Connection searchList(ConnectionManager.YSortedList list, int x, int y, int radius) {
        return list.searchForClosest(createConnection(x, y), radius);
    }

    private Connection createConnection(int x, int y) {
        Connection conn = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        conn.setPosition(x, y);
        return conn;
    }
}
