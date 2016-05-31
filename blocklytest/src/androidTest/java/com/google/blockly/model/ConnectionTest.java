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
    private Connection shadowInput;
    private Connection shadowOutput;
    private Connection shadowPrevious;
    private Connection shadowNext;
    private Block.Builder blockBuilder;

    public void setUp() {

        blockBuilder = new Block.Builder("dummyBlock");
        input = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        input.setBlock(blockBuilder.build());

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, null);
        output.setBlock(blockBuilder.build());

        previous = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        previous.setBlock(blockBuilder.build());

        next = new Connection(Connection.CONNECTION_TYPE_NEXT, null);
        next.setBlock(blockBuilder.build());

        blockBuilder.setShadow(true);
        shadowInput = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        shadowInput.setBlock(blockBuilder.build());

        shadowOutput = new Connection(Connection.CONNECTION_TYPE_OUTPUT, null);
        shadowOutput.setBlock(blockBuilder.build());

        shadowPrevious = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        shadowPrevious.setBlock(blockBuilder.build());

        shadowNext = new Connection(Connection.CONNECTION_TYPE_NEXT, null);
        shadowNext.setBlock(blockBuilder.build());

        blockBuilder.setShadow(false);
    }


    public void testCanConnectWithReason() {
        assertEquals(Connection.REASON_TARGET_NULL, input.canConnectWithReason(null));
        assertEquals(Connection.REASON_TARGET_NULL, input.canConnectWithReason(
                new Connection(Connection.CONNECTION_TYPE_OUTPUT, null)));

        assertEquals(Connection.REASON_SELF_CONNECTION, input.canConnectWithReason(input));
        assertEquals(Connection.REASON_SELF_CONNECTION, input.canConnectWithReason(input));
    }

    public void testCanConnectWithReasonDisconnect() {
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(output));
        Connection conn = new Connection(Connection.CONNECTION_TYPE_OUTPUT, null);
        conn.setBlock(blockBuilder.build());
        input.connect(conn);
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
        input.setBlock(blockBuilder.build());
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(output));

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"int"});
        output.setBlock(blockBuilder.build());
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(output));

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"String"});
        output.setBlock(blockBuilder.build());
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(output));

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"String", "int"});
        output.setBlock(blockBuilder.build());
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(output));

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"Some other type"});
        output.setBlock(blockBuilder.build());
        assertEquals(Connection.REASON_CHECKS_FAILED, input.canConnectWithReason(output));
    }

    public void testCanConnectWithReason_shadows() {
        // Verify a shadow can connect
        assertEquals(Connection.CAN_CONNECT, input.canConnectWithReason(shadowOutput));
        input.connect(output);
        // Verify a shadow and non shadow can't be connected at the same time
        assertEquals(Connection.REASON_MUST_DISCONNECT, input.canConnectWithReason(shadowOutput));
        input.disconnect();
        input.connect(shadowOutput);

        // Veryify a normal connection can't be made after a shadow connection
        next.connect(shadowPrevious);
        assertEquals(Connection.REASON_MUST_DISCONNECT, next.canConnectWithReason(previous));
    }

    public void testCheckConnection_failure() {
        try {
            input.checkConnection(input);
            fail("Connections cannot connect to themselves!");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        Connection input2 = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        input2.setBlock(blockBuilder.build());
        try {
            input.checkConnection(input2);
            fail("Input cannot connect to input!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            input.checkConnection(previous);
            fail("Input cannot connect to previous!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            input.checkConnection(next);
            fail("Input cannot connect to next!");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        Connection output2 = new Connection(Connection.CONNECTION_TYPE_OUTPUT, null);
        output2.setBlock(blockBuilder.build());
        try {
            output.checkConnection(output2);
            fail("Output cannot connect to output!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            output.checkConnection(previous);
            fail("Output cannot connect to previous!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            output.checkConnection(next);
            fail("Output cannot connect to next!");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        Connection previous2 = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        previous2.setBlock(blockBuilder.build());
        try {
            previous.checkConnection(previous2);
            fail("Previous cannot connect to previous!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            previous.checkConnection(input);
            fail("Previous cannot connect to input!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            previous.checkConnection(output);
            fail("Previous cannot connect to output!");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        Connection next2 = new Connection(Connection.CONNECTION_TYPE_NEXT, null);
        next2.setBlock(blockBuilder.build());
        try {
            next.checkConnection(next2);
            fail("Next cannot connect to next!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            next.checkConnection(input);
            fail("Next cannot connect to input!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            next.checkConnection(output);
            fail("Next cannot connect to output");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testCheckConnection_okay() {
        previous.checkConnection(next);
        next.checkConnection(previous);
        input.checkConnection(output);
        output.checkConnection(input);

        previous.checkConnection(shadowNext);
        next.checkConnection(shadowPrevious);
        input.checkConnection(shadowOutput);
        output.checkConnection(shadowInput);

        shadowPrevious.checkConnection(shadowNext);
        shadowNext.checkConnection(shadowPrevious);
        shadowInput.checkConnection(shadowOutput);
        shadowOutput.checkConnection(shadowInput);

        shadowPrevious.checkConnection(next);
        shadowNext.checkConnection(previous);
        shadowInput.checkConnection(output);
        shadowOutput.checkConnection(input);
    }

    public void testCheckConnection_shadowFailures() {
        // shadows hit the same checks as normal blocks.
        // Do light verification to guard against that changing
        try {
            shadowInput.checkConnection(shadowInput);
            fail("Connections cannot connect to themselves!");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        try {
            input.checkConnection(shadowInput);
            fail("Input cannot connect to input!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            input.checkConnection(shadowPrevious);
            fail("Input cannot connect to previous!");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        Connection shadowInput2 = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        blockBuilder.setShadow(true);
        shadowInput2.setBlock(blockBuilder.build());
        try {
            shadowInput.checkConnection(shadowInput2);
            fail("Input cannot connect to input!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            shadowInput.checkConnection(shadowNext);
            fail("Input cannot connect to next!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            shadowInput.checkConnection(shadowPrevious);
            fail("Input cannot connect to previous!");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
