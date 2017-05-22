package com.google.blockly.model.mutator;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link IfElseMutator}.
 */
public class IfElseMutatorTest extends BlocklyTestCase {
    private BlocklyController mController;
    private Block mBlock;
    private XmlPullParserFactory mXmlPullParserFactory;

    @Before
    public void setUp() throws Exception {
        configureForUIThread();

        mXmlPullParserFactory = XmlPullParserFactory.newInstance();
        mController = new BlocklyController.Builder(getContext())
                .addBlockDefinitionsFromAsset("default/test_blocks.json")
                .build();
        BlockFactory factory = mController.getBlockFactory();
        factory.registerMutator(IfElseMutator.MUTATOR_ID, IfElseMutator.FACTORY);

        mBlock = mController.getBlockFactory().obtainBlockFrom(
                new BlockTemplate("controls_if"));
    }

    @Test
    public void test_serialize() {

        runAndSync(new Runnable() {
            @Override
            public void run() {

                IfElseMutator mutator = (IfElseMutator) mBlock.getMutator();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                XmlSerializer serializer = null;
                try {
                    serializer = getXmlSerializer(os);
                } catch (BlocklySerializerException e) {
                    Truth.THROW_ASSERTION_ERROR.fail("Error getting serializer.", e);
                }

                try {
                    mutator.serialize(serializer);
                    serializer.flush();
                } catch (IOException e) {
                    Truth.THROW_ASSERTION_ERROR.fail("Error serializing mutator.", e);
                }
                assertThat(os.toString()).isEqualTo("");
                os.reset();

                String xml = "<mutation elseif=\"3\" else=\"1\" />";
                mutator.mutate(3, true);
                try {
                    mutator.serialize(serializer);
                    serializer.flush();
                } catch (IOException e) {
                    Truth.THROW_ASSERTION_ERROR.fail("Error serializing mutator.", e);
                }
                assertThat(os.toString()).isEqualTo(xml);
                os.reset();
            }
        });
    }

    @Test
    public void test_updateXml() {
        runAndSync(new Runnable() {
            @Override
            public void run() {

                IfElseMutator mutator = (IfElseMutator) mBlock.getMutator();
                assertWithMessage("Mutator should start with 0 else if count.")
                        .that(mutator.getElseIfCount()).isEqualTo(0);
                assertWithMessage("Mutator should start without an else.")
                        .that(mutator.hasElse()).isEqualTo(false);

                assertThat(mBlock.getInputByName("IF0")).isNotNull();
                assertThat(mBlock.getInputByName("DO0")).isNotNull();
                assertThat(mBlock.getInputByName("IF1")).isNull();
                assertThat(mBlock.getInputByName("DO1")).isNull();
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

    @Test
    public void test_updateValues() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mBlock.setEventWorkspaceId("Fake Workspace");  // Required for events to fire.

                IfElseMutator mutator = (IfElseMutator) mBlock.getMutator();

                assertWithMessage("Mutator should start with 0 else if count.")
                        .that(mutator.getElseIfCount()).isEqualTo(0);
                assertWithMessage("Mutator should start without an else.")
                        .that(mutator.hasElse()).isEqualTo(false);

                assertThat(mBlock.getInputByName("IF0")).isNotNull();
                assertThat(mBlock.getInputByName("DO0")).isNotNull();
                assertThat(mBlock.getInputByName("IF1")).isNull();
                assertThat(mBlock.getInputByName("DO1")).isNull();
                assertThat(mBlock.getInputByName("ELSE")).isNull();

                mutator.mutate(2, true);

                for (int i = 0; i <= 2; i++) {
                    assertThat(mBlock.getInputByName("IF" + i)).isNotNull();
                    assertThat(mBlock.getInputByName("DO" + i)).isNotNull();
                }
                assertThat(mBlock.getInputByName("ELSE")).isNotNull();
                assertThat(mBlock.getInputByName("IF3")).isNull();
                assertThat(mBlock.getInputByName("DO3")).isNull();

                mutator.mutate(1, false);
                for (int i = 0; i <= 1; i++) {
                    assertThat(mBlock.getInputByName("IF" + i)).isNotNull();
                    assertThat(mBlock.getInputByName("DO" + i)).isNotNull();
                }

                assertThat(mBlock.getInputByName("IF2")).isNull();
                assertThat(mBlock.getInputByName("DO2")).isNull();
                assertThat(mBlock.getInputByName("ELSE")).isNull();

            }
        });
    }

    private XmlSerializer getXmlSerializer(ByteArrayOutputStream os)
            throws BlocklySerializerException
    {
        XmlSerializer serializer;
        try {
            mXmlPullParserFactory.setNamespaceAware(true);
            serializer = mXmlPullParserFactory.newSerializer();
            serializer.setOutput(os, null);
            return serializer;
        } catch (XmlPullParserException | IOException e) {
            throw new BlocklySerializerException(e);
        }
    }
}
