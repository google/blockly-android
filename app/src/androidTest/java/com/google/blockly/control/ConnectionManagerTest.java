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

    private Connection createConnection(int x, int y) {
        Connection conn = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        conn.setPosition(x, y);
        return conn;
    }
}
