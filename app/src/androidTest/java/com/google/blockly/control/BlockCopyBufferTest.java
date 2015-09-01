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

import com.google.blockly.R;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link BlockCopyBuffer}
 */
public class BlockCopyBufferTest extends AndroidTestCase {
    private BlockFactory bf;
    private BlockCopyBuffer buffer;
    ArrayList<Block> inputList;
    List<Block> outputList;

    @Override
    public void setUp() throws Exception {
        bf = new BlockFactory(getContext(), new int[] {R.raw.test_blocks});
        buffer = new BlockCopyBuffer();
        inputList = new ArrayList<>();
        outputList = null;
    }

    public void testBuffer() throws BlocklySerializerException {
        Block block = bf.obtainBlock("frankenblock", "testBlock");
        inputList.add(block);
        buffer.setBufferContents(inputList);
        Block fromBuffer = buffer.getBufferContents(bf).get(0);
        assertNotNull(fromBuffer);
        assertEquals("frankenblock", fromBuffer.getName());
        assertNotSame(fromBuffer, block);
    }

    public void testWithInput() throws BlocklySerializerException {
        Block block = bf.obtainBlock("frankenblock", "testBlock");
        block.setPosition(37, 13);

        Input input = block.getInputByName("value_input");
        Block inputBlock = bf.obtainBlock("output_foo", "126");
        input.getConnection().connect(inputBlock.getOutputConnection());

        inputList.add(block);
        buffer.setBufferContents(inputList);

        outputList = buffer.getBufferContents(bf);
        assertNotNull(outputList);
        assertFalse(outputList.isEmpty());

        Block fromBuffer = outputList.get(0);
        assertEquals(37, fromBuffer.getPosition().x);
        assertEquals(13, fromBuffer.getPosition().y);

        assertEquals(3, fromBuffer.getInputs().size());
        Block inputFromBuffer = fromBuffer.getInputByName("value_input")
                .getConnection()
                .getTargetBlock();
        assertEquals("output_foo", inputFromBuffer.getName());
    }

    public void testNextBlock() throws BlocklySerializerException {
        Block block = bf.obtainBlock("frankenblock", "testBlock");
        block.setPosition(37, 13);

        Block nextBlock = bf.obtainBlock("frankenblock", "nextBlock");
        nextBlock.setPosition(100, 1000);
        block.getNextConnection().connect(nextBlock.getPreviousConnection());

        nextBlock.getFieldByName("text_input").setFromXmlText("expected_text");

        inputList.add(block);
        inputList.add(nextBlock);
        buffer.setBufferContents(inputList);

        outputList = buffer.getBufferContents(bf);
        assertNotNull(outputList);
        assertFalse(outputList.isEmpty());

        Block fromBuffer = outputList.get(0);
        assertEquals(37, fromBuffer.getPosition().x);
        assertEquals(13, fromBuffer.getPosition().y);

        Block nextFromBuffer = fromBuffer.getNextBlock();
        // Next block isn't a top level block, so it doesn't store its location.
        assertEquals(0, nextFromBuffer.getPosition().x);
        assertEquals(0, nextFromBuffer.getPosition().y);
        assertEquals("expected_text",
                ((Field.FieldInput) nextFromBuffer.getFieldByName("text_input")).getText());
    }
}
