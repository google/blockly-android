package com.google.blockly.model.mutator;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.model.DefaultBlocks;
import com.google.blockly.model.FieldDropdown;
import com.google.blockly.utils.BlockLoadingException;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link MathIsDivisibleByMutator}.
 */
public class MathIsDivisibleByMutatorTest extends BlocklyTestCase {
    BlocklyController mController;
    Block mBlock;

    @Before
    public void setUp() throws BlockLoadingException {
        configureForUIThread();

        mController = new BlocklyController.Builder(getContext())
                .addBlockDefinitionsFromAsset(DefaultBlocks.MATH_BLOCKS_PATH)
                .build();
        BlockFactory factory = mController.getBlockFactory();
        factory.registerMutator(
                MathIsDivisibleByMutator.MUTATOR_ID,
                MathIsDivisibleByMutator.FACTORY);
        mBlock = mController.getBlockFactory().obtainBlockFrom(
                new BlockTemplate("math_number_property"));
    }

    @Test
    public void test() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mBlock.setEventWorkspaceId("Fake Workspace");  // Required for events to fire.

                MathIsDivisibleByMutator mutator = (MathIsDivisibleByMutator) mBlock.getMutator();
                assertWithMessage("Mutator must find FieldDropdown \"PROPERTY\"")
                        .that(mutator.mDropdown).isNotNull();

                // Only "DIVISIBLE_BY" should append the input.
                FieldDropdown.Options options = mutator.mDropdown.getOptions();
                for (FieldDropdown.Option option : options.mOptionList) {
                    if (!option.value.equals("DIVISIBLE_BY")) {
                        mutator.mDropdown.setFromString(option.value);
                        assertThat(mBlock.getInputByName("DIVISOR")).isNull();

                        mutator.mDropdown.setFromString("DIVISIBLE_BY");
                        assertThat(mBlock.getInputByName("DIVISOR")).isNotNull();
                    }
                }
            }
        });
    }
}
