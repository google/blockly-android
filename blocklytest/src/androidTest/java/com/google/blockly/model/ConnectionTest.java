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

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static com.google.blockly.model.Connection.CAN_CONNECT;
import static com.google.blockly.model.Connection.CONNECTION_TYPE_OUTPUT;
import static com.google.blockly.model.Connection.REASON_CHECKS_FAILED;
import static com.google.blockly.model.Connection.REASON_MUST_DISCONNECT;
import static com.google.blockly.model.Connection.REASON_SELF_CONNECTION;
import static com.google.blockly.model.Connection.REASON_TARGET_NULL;
import static com.google.blockly.model.Connection.REASON_WRONG_TYPE;

import static com.google.blockly.utils.ConnectionSubject.assertThat;


/**
 * Tests for {@link Connection}.
 */
public class ConnectionTest {
    private BlocklyController mMockController;

    private BlockFactory factory;
    private BlockTemplate dummyBlock;
    private BlockTemplate shadowBlock;

    private Connection input;
    private Connection output;
    private Connection previous;
    private Connection next;
    private Connection shadowInput;
    private Connection shadowOutput;
    private Connection shadowPrevious;
    private Connection shadowNext;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws JSONException, BlockLoadingException {
        mMockController = Mockito.mock(BlocklyController.class);

        factory = new BlockFactory();
        factory.setController(mMockController);
        factory.addDefinition(new BlockDefinition("{\"type\": \"dummyBlock\"}"));
        dummyBlock = new BlockTemplate().ofType("dummyBlock");
        shadowBlock = new BlockTemplate(dummyBlock).shadow();

        input = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        input.setBlock(factory.obtainBlockFrom(dummyBlock));

        output = new Connection(CONNECTION_TYPE_OUTPUT, null);
        output.setBlock(factory.obtainBlockFrom(dummyBlock));

        previous = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        previous.setBlock(factory.obtainBlockFrom(dummyBlock));

        next = new Connection(Connection.CONNECTION_TYPE_NEXT, null);
        next.setBlock(factory.obtainBlockFrom(dummyBlock));

        shadowInput = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        shadowInput.setBlock(factory.obtainBlockFrom(shadowBlock));

        shadowOutput = new Connection(CONNECTION_TYPE_OUTPUT, null);
        shadowOutput.setBlock(factory.obtainBlockFrom(shadowBlock));

        shadowPrevious = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        shadowPrevious.setBlock(factory.obtainBlockFrom(shadowBlock));

        shadowNext = new Connection(Connection.CONNECTION_TYPE_NEXT, null);
        shadowNext.setBlock(factory.obtainBlockFrom(shadowBlock));
    }

    @Test
    public void testCanConnectWithReason() {
        assertThat(input).connectingTo(null).returnsReason(REASON_TARGET_NULL);
        assertThat(input)
                .connectingTo(new Connection(CONNECTION_TYPE_OUTPUT, null))
                .returnsReason(REASON_TARGET_NULL);

        assertThat(input).connectingTo(input).returnsReason(REASON_SELF_CONNECTION);
    }

    @Test
    public void testCanConnectWithReasonDisconnect() throws BlockLoadingException {
        assertThat(input).connectingTo(output).isSuccessful();

        Connection conn = new Connection(CONNECTION_TYPE_OUTPUT, null);
        conn.setBlock(factory.obtainBlockFrom(dummyBlock));
        input.connect(conn);
        assertThat(input).connectingTo(output).returnsReason(REASON_MUST_DISCONNECT);
    }

    @Test
    public void testCanConnectWithReasonWrongType() {
        assertThat(input).connectingTo(previous).returnsReason(REASON_WRONG_TYPE);
        assertThat(input).connectingTo(next).returnsReason(REASON_WRONG_TYPE);

        assertThat(output).connectingTo(previous).returnsReason(REASON_WRONG_TYPE);
        assertThat(output).connectingTo(next).returnsReason(REASON_WRONG_TYPE);

        assertThat(previous).connectingTo(input).returnsReason(REASON_WRONG_TYPE);
        assertThat(previous).connectingTo(output).returnsReason(REASON_WRONG_TYPE);

        assertThat(next).connectingTo(input).returnsReason(REASON_WRONG_TYPE);
        assertThat(next).connectingTo(output).returnsReason(REASON_WRONG_TYPE);
    }

    @Test
    public void testCanConnectWithReasonChecks() throws BlockLoadingException {
        input = new Connection(Connection.CONNECTION_TYPE_INPUT, new String[]{"String", "int"});
        input.setBlock(factory.obtainBlockFrom(dummyBlock));
        assertThat(input).connectingTo(output).returnsReason(CAN_CONNECT);

        output = new Connection(CONNECTION_TYPE_OUTPUT, new String[]{"int"});
        output.setBlock(factory.obtainBlockFrom(dummyBlock));
        assertThat(input).connectingTo(output).returnsReason(CAN_CONNECT);

        output = new Connection(CONNECTION_TYPE_OUTPUT, new String[]{"String"});
        output.setBlock(factory.obtainBlockFrom(dummyBlock));
        assertThat(input).connectingTo(output).returnsReason(CAN_CONNECT);

        output = new Connection(CONNECTION_TYPE_OUTPUT, new String[]{"String", "int"});
        output.setBlock(factory.obtainBlockFrom(dummyBlock));
        assertThat(input).connectingTo(output).returnsReason(CAN_CONNECT);

        output = new Connection(CONNECTION_TYPE_OUTPUT, new String[]{"Some other type"});
        output.setBlock(factory.obtainBlockFrom(dummyBlock));
        assertThat(input).connectingTo(output).returnsReason(REASON_CHECKS_FAILED);
    }

    @Test
    public void testCanConnectWithReason_shadows() {
        // Verify a shadow can connect
        assertThat(input).connectingTo(shadowOutput).isSuccessful();
        input.connect(output);
        // Verify a shadow and non shadow can't be connected at the same time
        assertThat(input).connectingTo(shadowOutput).returnsReason(REASON_MUST_DISCONNECT);
        input.disconnect();
        input.connect(shadowOutput);

        // Veryify a normal connection can't be made after a shadow connection
        next.connect(shadowPrevious);
        assertThat(next).connectingTo(previous).returnsReason(REASON_MUST_DISCONNECT);
    }

    @Test
    public void testCheckConnection_failOnInputCannotConnectToSelf() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Connections cannot connect to themselves!");
        input.checkConnection(input);
    }

    @Test
    public void testCheckConnection_failsOnInputCannotConnectToInput()
            throws BlockLoadingException {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Input cannot connect to input!");

        Connection input2 = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        input2.setBlock(factory.obtainBlockFrom(dummyBlock));

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
    public void testCheckConnection_failOnOutputCannotConnectToBlock()
            throws BlockLoadingException {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Output cannot connect to output!");

        Connection output2 = new Connection(CONNECTION_TYPE_OUTPUT, null);
        output2.setBlock(factory.obtainBlockFrom(dummyBlock));
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
    public void testCheckConnection_failOnPreviousCannotConnectToPrevious()
            throws BlockLoadingException {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Previous cannot connect to previous!");

        Connection previous2 = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null);
        previous2.setBlock(factory.obtainBlockFrom(dummyBlock));
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
    public void testCheckConnection_failOnNextCannotConnectToNext() throws BlockLoadingException {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Next cannot connect to next!");

        Connection next2 = new Connection(Connection.CONNECTION_TYPE_NEXT, null);
        next2.setBlock(factory.obtainBlockFrom(dummyBlock));
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
    public void testCheckConnection_failOnShadowInputCannotConnectToShadowInput()
            throws BlockLoadingException {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Input cannot connect to input!");

        Connection shadowInput2 = new Connection(Connection.CONNECTION_TYPE_INPUT, null);
        shadowInput2.setBlock(factory.obtainBlockFrom(shadowBlock));

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
