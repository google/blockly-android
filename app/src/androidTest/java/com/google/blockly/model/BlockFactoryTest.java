package com.google.blockly.model;

import android.test.AndroidTestCase;

import com.google.blockly.R;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@link BlockFactory}.
 */
public class BlockFactoryTest extends AndroidTestCase {

    // TODO: Move rest_blocks.json to the testapp's resources once
    // https://code.google.com/p/android/issues/detail?id=64887 is fixed.

    public void testLoadBlocks() {
        BlockFactory bf = new BlockFactory(getContext(), new int[] {R.raw.test_blocks});
        List<Block> blocks = bf.getAllBlocks();
        assertEquals("BlockFactory failed to load all blocks.", 5, blocks.size());
    }

    public void testObtainBlock() {
        BlockFactory bf = new BlockFactory(getContext(), new int[] {R.raw.test_blocks});
        Block emptyBlock = bf.obtainBlock("empty_block");
        assertNotNull("Failed to create the empty block.", emptyBlock);
        assertEquals("Empty block has the wrong name", "empty_block", emptyBlock.getName());

        Block frankenblock = bf.obtainBlock("frankenblock");
        assertNotNull("Failed to create the frankenblock.", frankenblock);

        List<Input> inputs = frankenblock.getInputs();
        assertEquals("Frankenblock has the wrong number of inputs", 3, inputs.size());
        assertTrue("First input should be a value input.",
                inputs.get(0) instanceof Input.InputValue);
        assertTrue("Second input should be a statement input.",
                inputs.get(1) instanceof Input.InputStatement);
        assertTrue("Third input should be a dummy input.",
                inputs.get(2) instanceof Input.InputDummy);

        assertNotNull(frankenblock.getFieldByName("angle"));
    }

    public void testObtainRepeated() {
        BlockFactory bf = new BlockFactory(getContext(), new int[] {R.raw.test_blocks});
        Block frankenblock = bf.obtainBlock("frankenblock");
        assertNotNull("Failed to create the frankenblock.", frankenblock);

        Block frankencopy = bf.obtainBlock("frankenblock");
        assertNotSame("Obtained blocks should be distinct objects.", frankenblock, frankencopy);

        assertNotSame("Obtained blocks should not share connections.",
                frankenblock.getNextConnection(), frankencopy.getNextConnection());
        assertNotSame("Obtained blocks should not share connections.",
                frankenblock.getPreviousConnection(), frankencopy.getPreviousConnection());
        assertNull(frankenblock.getOutputConnection());
        assertNotSame("Obtained blocks should not share inputs.",
                frankenblock.getInputs().get(0), frankencopy.getInputs().get(0));
    }

}
