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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link Connection}.
 */
public class ConnectionTest {
    private Connection input;
    private Connection output;
    private Connection previous;
    private Connection next;
    private Connection shadowInput;
    private Connection shadowOutput;
    private Connection shadowPrevious;
    private Connection shadowNext;
    private Block.Builder blockBuilder;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
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

    @Test
    public void testCanConnectWithReason() {
        assertThat(input.canConnectWithReason(null)).isEqualTo(Connection.REASON_TARGET_NULL);
        assertThat(input.canConnectWithReason(
            new Connection(Connection.CONNECTION_TYPE_OUTPUT, null)))
            .isEqualTo(Connection.REASON_TARGET_NULL);

        assertThat(input.canConnectWithReason(input)).isEqualTo(Connection.REASON_SELF_CONNECTION);
        assertThat(input.canConnectWithReason(input)).isEqualTo(Connection.REASON_SELF_CONNECTION);
    }

    @Test
    public void testCanConnectWithReasonDisconnect() {
        assertThat(input.canConnectWithReason(output)).isEqualTo(Connection.CAN_CONNECT);
        Connection conn = new Connection(Connection.CONNECTION_TYPE_OUTPUT, null);
        conn.setBlock(blockBuilder.build());
        input.connect(conn);
        assertThat(input.canConnectWithReason(output)).isEqualTo(Connection.REASON_MUST_DISCONNECT);
    }

    @Test
    public void testCanConnectWithReasonType() {
        assertThat(input.canConnectWithReason(previous)).isEqualTo(Connection.REASON_WRONG_TYPE);
        assertThat(input.canConnectWithReason(next)).isEqualTo(Connection.REASON_WRONG_TYPE);

        assertThat(output.canConnectWithReason(previous)).isEqualTo(Connection.REASON_WRONG_TYPE);
        assertThat(output.canConnectWithReason(next)).isEqualTo(Connection.REASON_WRONG_TYPE);

        assertThat(previous.canConnectWithReason(input)).isEqualTo(Connection.REASON_WRONG_TYPE);
        assertThat(previous.canConnectWithReason(output)).isEqualTo(Connection.REASON_WRONG_TYPE);

        assertThat(next.canConnectWithReason(input)).isEqualTo(Connection.REASON_WRONG_TYPE);
        assertThat(next.canConnectWithReason(output)).isEqualTo(Connection.REASON_WRONG_TYPE);
    }

    @Test
    public void testCanConnectWithReasonChecks() {
        input = new Connection(Connection.CONNECTION_TYPE_INPUT, new String[]{"String", "int"});
        input.setBlock(blockBuilder.build());
        assertThat(input.canConnectWithReason(output)).isEqualTo(Connection.CAN_CONNECT);

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"int"});
        output.setBlock(blockBuilder.build());
        assertThat(input.canConnectWithReason(output)).isEqualTo(Connection.CAN_CONNECT);

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"String"});
        output.setBlock(blockBuilder.build());
        assertThat(input.canConnectWithReason(output)).isEqualTo(Connection.CAN_CONNECT);

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"String", "int"});
        output.setBlock(blockBuilder.build());
        assertThat(input.canConnectWithReason(output)).isEqualTo(Connection.CAN_CONNECT);

        output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, new String[]{"Some other type"});
        output.setBlock(blockBuilder.build());
        assertThat(input.canConnectWithReason(output)).isEqualTo(Connection.REASON_CHECKS_FAILED);
    }

    @Test
    public void testCanConnectWithReason_shadows() {
        // Verify a shadow can connect
        assertThat(input.canConnectWithReason(shadowOutput)).isEqualTo(Connection.CAN_CONNECT);
        input.connect(output);
        // Verify a shadow and non shadow can't be connected at the same time
        assertThat(input.canConnectWithReason(shadowOutput))
            .isEqualTo(Connection.REASON_MUST_DISCONNECT);
        input.disconnect();
        input.connect(shadowOutput);

        // Veryify a normal connection can't be made after a shadow connection
        next.connect(shadowPrevious);
        assertThat(next.canConnectWithReason(previous))
            .isEqualTo(Connection.REASON_MUST_DISCONNECT);
    }

    @Test
    public void testCheckConnection_failOnInputCannotConnectToSelf() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Connections cannot connect to themselves!");
        input.checkConnection(input);
    }

    @Test
    public void testCheckConnection_failsOnInputCannotConnectToInput() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Input cannot connect to input!");

        Connection input2 = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        input2.setBlock(blockBuilder.build());

        input.checkConnection(input2);
    }

    @Test
    public void testCheckConnection_failOnInputCannotConnectionToPrevious() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Input cannot connect to previous!");
        input.checkConnection(previous);
    }

    @Test
    public void testCheckConnection_failOnInputCannotConnectToNext() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Input cannot connect to next!");
        input.checkConnection(next);
    }

    @Test
    public void testCheckConnection_failOnOutputCannotConnectToBlock() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Output cannot connect to output!");

        Connection output2 = new Connection(Connection.CONNECTION_TYPE_OUTPUT, null);
        output2.setBlock(blockBuilder.build());
        output.checkConnection(output2);
    }

    @Test
    public void testCheckConnection_failOnOutputCannotConnectToPrevious() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Output cannot connect to previous!");
        output.checkConnection(previous);
    }

    @Test
    public void testCheckConnection_failOnOutputCannotConnectToNext() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Output cannot connect to next!");
        output.checkConnection(next);
    }

    @Test
    public void testCheckConnection_failOnPreviousCannotConnectToPrevious() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Previous cannot connect to previous!");

        Connection previous2 = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        previous2.setBlock(blockBuilder.build());
        previous.checkConnection(previous2);
    }

    @Test
    public void testCheckConnection_failOnPreviousCannotConnectToInput() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Previous cannot connect to input!");
        previous.checkConnection(input);
    }

    @Test
    public void testCheckConnection_failOnPreviousCannotConnectToOutput() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Previous cannot connect to output!");
        previous.checkConnection(output);
    }

    @Test
    public void testCheckConnection_failOnNextCannotConnectToNext() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Next cannot connect to next!");

        Connection next2 = new Connection(Connection.CONNECTION_TYPE_NEXT, null);
        next2.setBlock(blockBuilder.build());
        next.checkConnection(next2);
    }

    @Test
    public void testCheckConnection_failOnNextCannotConnectToInput() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Next cannot connect to input!");
        next.checkConnection(input);
    }

    @Test
    public void testCheckConnection_failOnNextCannotConnectToOutput() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Next cannot connect to output");
        next.checkConnection(output);
    }

    @Test
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

    // shadows hit the same checks as normal blocks.
    // Do light verification to guard against that changing

    @Test
    public void testCheckConnection_failOnShadowInputCannotConnectToSelf() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Connections cannot connect to themselves!");
        shadowInput.checkConnection(shadowInput);
    }

    @Test
    public void testCheckConnection_failOnInputCannotConnectToShadowInput() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Input cannot connect to input!");
        input.checkConnection(shadowInput);
    }

    @Test
    public void testCheckConnection_failOnInputCannotConnectToShadowPrevious() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Input cannot connect to previous!");
        input.checkConnection(shadowPrevious);
    }

    @Test
    public void testCheckConnection_failOnShadowInputCannotConnectToShadowInput() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Input cannot connect to input!");

        Connection shadowInput2 = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        blockBuilder.setShadow(true);
        shadowInput2.setBlock(blockBuilder.build());

        shadowInput.checkConnection(shadowInput2);
    }

    @Test
    public void testCheckConnection_failOnShadowInputCannotConnectToShadowNext() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Input cannot connect to next!");
        shadowInput.checkConnection(shadowNext);
    }

    @Test
    public void testCheckConnection_failOnShadowInputCannotConnectToShadowPrevious() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Input cannot connect to previous!");
        shadowInput.checkConnection(shadowPrevious);
    }
}
