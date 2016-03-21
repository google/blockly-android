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
package com.google.blockly.model;

import android.test.AndroidTestCase;

/**
 * Tests for {@link Connection}.
 */
public class ConnectionTest extends AndroidTestCase {
    private Connection input;
    private Connection output;
    private Connection previous;
    private Connection next;

    public void setUp() {
        input = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, null);
        previous = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        next = new Connection(Connection.CONNECTION_TYPE_NEXT, null);
    }


    public void testCanConnectWithReason() {
        assertEquals(Connection.REASON_TARGET_NULL, input.canConnectWithReason(null));

        Block block = new Block.Builder("Dummy block").build();
        input.setBlock(block);
        assertEquals(Connection.REASON_SELF_CONNECTION, input.canConnectWithReason(input));
        assertEquals(Connection.REASON_SELF_CONNECTION, input.canConnectWithReason(input));
    }

    public void testCanConnectWithReasonDisconnect() {
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(output));
        input.connect(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));
        assertEquals(Connection.REASON_MUST_DISCONNECT, input.canConnectWithReason(output));
    }

    public void testCanConnectWithReasonType() {
        assertEquals(Connection.REASON_WRONG_TYPE, input.canConnectWithReason(previous));
        assertEquals(Connection.REASON_WRONG_TYPE, input.canConnectWithReason(next));

        assertEquals(Connection.REASON_WRONG_TYPE, output.canConnectWithReason(previous));
        assertEquals(Connection.REASON_WRONG_TYPE, output.canConnectWithReason(next));

        assertEquals(Connection.REASON_WRONG_TYPE, previous.canConnectWithReason(input));
        assertEquals(Connection.REASON_WRONG_TYPE, previous.canConnectWithReason(output));

        assertEquals(Connection.REASON_WRONG_TYPE, next.canConnectWithReason(input));
        assertEquals(Connection.REASON_WRONG_TYPE, next.canConnectWithReason(output));
    }


    public void testCanConnectWithReasonChecks() {
        input = new Connection(Connection.CONNECTION_TYPE_INPUT, new String[]{"String", "int"});
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(output));

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"int"});
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(output));

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"String"});
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(output));

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"String", "int"});
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(output));

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"Some other type"});
        assertEquals(Connection.REASON_CHECKS_FAILED, input.canConnectWithReason(output));
    }

    public void testCheckConnection_Self() {
        Block block = new Block.Builder("Dummy block").build();
        input.setBlock(block);

        try {
            input.checkConnection(input);
            fail();
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testCheckConnection_TypeInputPrev() {
        try {
            input.checkConnection(previous);
            fail();
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testCheckConnection_TypeInputNext() {
        try {
            input.checkConnection(next);
            fail();
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testCheckConnection_TypeOutputPrev() {
        try {
            output.checkConnection(previous);
            fail();
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testCheckConnection_TypePrevInput() {
        try {
            previous.checkConnection(input);
            fail();
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testCheckConnection_TypePrevOutput() {
        try {
            previous.checkConnection(input);
            fail();
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testCheckConnection_TypeNextInput() {
        try {
            next.checkConnection(input);
            fail();
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testCheckConnection_TypeNextOutput() {
        try {
            next.checkConnection(input);
            fail();
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testCheckConnection_okay() {
        previous.checkConnection(next);
        next.checkConnection(previous);
        input.checkConnection(output);
        output.checkConnection(input);
    }
}
