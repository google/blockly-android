package com.google.blockly.model.mutator;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.model.DefaultBlocks;
import com.google.blockly.model.FieldDropdown;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link IfElseMutator}.
 */
public class IfElseMutatorTest extends BlocklyTestCase {
    BlocklyController mController;
    Block mBlock;

    @Before
    public void setUp() throws BlockLoadingException {
        configureForUIThread();

        mController = new BlocklyController.Builder(getContext())
                .addBlockDefinitionsFromAsset(DefaultBlocks.LOGIC_BLOCKS_PATH)
                .build();
        BlockFactory factory = mController.getBlockFactory();
        factory.registerMutator(
                IfElseMutator.MUTATOR_ID,
                new IfElseMutator.Factory(getContext(), mController));
        mBlock = mController.getBlockFactory().obtainBlockFrom(
                new BlockTemplate("controls_if"));
    }

    @Test
    public void test_update() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mBlock.setEventWorkspaceId("Fake Workspace");  // Required for events to fire.

                IfElseMutator mutator = (IfElseMutator) mBlock.getMutator();

                assertWithMessage("Mutator should have a UI.")
                        .that(mutator.hasUI()).isEqualTo(true);
                assertWithMessage("Mutator should start with 0 else if count.")
                        .that(mutator.getElseIfCount()).isEqualTo(0);
                assertWithMessage("Mutator should start without an else.")
                        .that(mutator.hasElse()).isEqualTo(false);
                assertWithMessage("Mutator should be able to create a fragment.")
                        .that(mutator.getMutatorFragment()).isNotNull();

                assertThat(mBlock.getInputByName("IF0")).isNotNull();
                assertThat(mBlock.getInputByName("DO0")).isNotNull();
                assertThat(mBlock.getInputByName("IF1")).isNull();
                assertThat(mBlock.getInputByName("DO1")).isNull();
                assertThat(mBlock.getInputByName("ELSE")).isNull();

                mutator.update(2, true);

                for (int i = 0; i <= 2; i++) {
                    assertThat(mBlock.getInputByName("IF" + i)).isNotNull();
                    assertThat(mBlock.getInputByName("DO" + i)).isNotNull();
                }
                assertThat(mBlock.getInputByName("ELSE")).isNotNull();
                assertThat(mBlock.getInputByName("IF3")).isNull();
                assertThat(mBlock.getInputByName("DO3")).isNull();

                mutator.update(1, false);
                for (int i = 0; i <= 1; i++) {
                    assertThat(mBlock.getInputByName("IF" + i)).isNotNull();
                    assertThat(mBlock.getInputByName("DO" + i)).isNotNull();
                }

                assertThat(mBlock.getInputByName("IF2")).isNull();
                assertThat(mBlock.getInputByName("DO2")).isNull();
                assertThat(mBlock.getInputByName("ELSE")).isNull();

                String xml = "<mutation elseif=\"3\" else=\"0\"></mutation>";
                try {
                    BlocklyXmlHelper.updateMutator(mBlock, mutator, xml);
                } catch (BlockLoadingException e) {
                    Truth.THROW_ASSERTION_ERROR.fail("Error updating from xml.", e);
                }
                for (int i = 0; i <= 3; i++) {
                    assertThat(mBlock.getInputByName("IF" + i)).isNotNull();
                    assertThat(mBlock.getInputByName("DO" + i)).isNotNull();
                }

                assertThat(mBlock.getInputByName("IF4")).isNull();
                assertThat(mBlock.getInputByName("DO4")).isNull();
                assertThat(mBlock.getInputByName("ELSE")).isNull();

                xml = "<mutation elseif=\"1\" else=\"1\"></mutation>";
                try {
                    BlocklyXmlHelper.updateMutator(mBlock, mutator, xml);
                } catch (BlockLoadingException e) {
                    Truth.THROW_ASSERTION_ERROR.fail("Error updating from xml.", e);
                }

                for (int i = 0; i <= 1; i++) {
                    assertThat(mBlock.getInputByName("IF" + i)).isNotNull();
                    assertThat(mBlock.getInputByName("DO" + i)).isNotNull();
                }

                assertThat(mBlock.getInputByName("IF2")).isNull();
                assertThat(mBlock.getInputByName("DO2")).isNull();
                assertThat(mBlock.getInputByName("ELSE")).isNotNull();
            }
        });
    }
}
