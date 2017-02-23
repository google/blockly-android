package com.google.blockly.model;

import android.support.test.InstrumentationRegistry;

import com.google.blockly.android.test.R;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link BlocklyEvent} classes.
 */
public class BlocklyEventTest {
    private static final String BLOCK_TYPE = "controls_whileUntil";
    private static final String BLOCK_ID = "block";
    private static final String FIELD_NAME = "MODE";
    private static final String WORKSPACE_ID = "workspace";

    private static final int NEW_POSITION_X = 123;
    private static final int NEW_POSITION_Y = 456;
    private static final WorkspacePoint NEW_POSITION =
            new WorkspacePoint(NEW_POSITION_X, NEW_POSITION_Y);

    private Workspace mMockWorkspace;
    private BlockFactory mBlockFactory;
    private Block mBlock;
    private Field mField;

    @Before
    public void setUp() throws Exception {
        mMockWorkspace = mock(Workspace.class);
        // TODO(#435): Replace R.raw.test_blocks
        mBlockFactory = new BlockFactory(InstrumentationRegistry.getContext(),
                new int[]{R.raw.test_blocks});
        mBlock = mBlockFactory.obtainBlock(BLOCK_TYPE, BLOCK_ID);
        mField = mBlock.getFieldByName(FIELD_NAME);
        mBlock.setPosition(NEW_POSITION_X, NEW_POSITION_Y);


        Mockito.when(mMockWorkspace.getId()).thenReturn(WORKSPACE_ID);
    }

    @Test
    public void testChangeEvent_collapsedState() throws JSONException {
        boolean newValue = mBlock.isCollapsed();
        boolean oldValue = !newValue;
        BlocklyEvent.ChangeEvent event =
                BlocklyEvent.ChangeEvent.newCollapsedStateEvent(mMockWorkspace, mBlock);
        testChangeEvent(BlocklyEvent.ELEMENT_COLLAPSED, event,
                        oldValue ? "true" : "false",
                        newValue ? "true" : "false");
    }

    @Test
    public void testChangeEvent_commentText() throws JSONException {
        String oldValue = "old comment";
        String newValue = "new comment";
        BlocklyEvent.ChangeEvent event = BlocklyEvent.ChangeEvent.newCommentTextEvent(
                mMockWorkspace, mBlock, oldValue, newValue);
        testChangeEvent(BlocklyEvent.ELEMENT_COMMENT, event, oldValue, newValue);
    }

    @Test
    public void testChangeEvent_disabledState() throws JSONException {
        boolean newValue = mBlock.isDisabled();
        boolean oldValue = !newValue;
        BlocklyEvent.ChangeEvent event =
                BlocklyEvent.ChangeEvent.newDisabledStateEvent(mMockWorkspace, mBlock);
        testChangeEvent(BlocklyEvent.ELEMENT_DISABLED, event,
                oldValue ? "true" : "false",
                newValue ? "true" : "false");
    }

    @Test
    public void testChangeEvent_fieldValue() throws JSONException {
        String oldValue = "UNTIL";
        String newValue = "WHILE";
        mField.setFromString(newValue);
        BlocklyEvent.ChangeEvent event = BlocklyEvent.ChangeEvent.newFieldValueEvent(
                mMockWorkspace, mBlock, mField, oldValue, newValue);
        testChangeEvent(BlocklyEvent.ELEMENT_FIELD, event, oldValue, newValue);
    }

    @Test
    public void testChangeEvent_inlineState() throws JSONException {
        boolean newValue = mBlock.getInputsInline();
        boolean oldValue = !newValue;
        BlocklyEvent.ChangeEvent event =
                BlocklyEvent.ChangeEvent.newInlineStateEvent(mMockWorkspace, mBlock);
        testChangeEvent(BlocklyEvent.ELEMENT_INLINE, event,
                oldValue ? "true" : "false",
                newValue ? "true" : "false");
    }

    @Test
    public void testChangeEvent_mutation() throws JSONException {
        String oldValue = "old mutation";
        String newValue = "new mutation";
        BlocklyEvent.ChangeEvent event = BlocklyEvent.ChangeEvent.newMutateEvent(
                mMockWorkspace, mBlock, oldValue, newValue);
        testChangeEvent(BlocklyEvent.ELEMENT_MUTATE, event, oldValue, newValue);
    }

    private void testChangeEvent(@BlocklyEvent.ChangeElement String element,
                                 BlocklyEvent.ChangeEvent event,
                                 String oldValue, String newValue)
            throws JSONException {
        assertThat(event.getTypeId()).isSameAs(BlocklyEvent.TYPE_CHANGE);
        assertThat(event.getTypeName()).isSameAs(BlocklyEvent.TYPENAME_CHANGE);
        assertThat(event.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        // Group id is assigned by the BlocklyController, not tested here.
        assertThat(event.getElement()).isEqualTo(element);
        assertThat(event.getOldValue()).isEqualTo(oldValue);
        assertThat(event.getNewValue()).isEqualTo(newValue);
        if (element == BlocklyEvent.ELEMENT_FIELD) {
            assertThat(event.getFieldName()).isEqualTo(FIELD_NAME);
        }

        String serialized = event.toJsonString();
        JSONObject json = new JSONObject(serialized);

        BlocklyEvent.ChangeEvent deserializedEvent =
                (BlocklyEvent.ChangeEvent) BlocklyEvent.fromJson(json);
        assertThat(deserializedEvent.getTypeName()).isEqualTo(event.getTypeName());
        // Workspace ids are not serialized.
        // Group id is assigned by the BlocklyController, not tested here.
        assertThat(deserializedEvent.getElement()).isEqualTo(element);
        assertThat(deserializedEvent.getBlockId()).isEqualTo(BLOCK_ID);
        assertThat(deserializedEvent.getNewValue()).isEqualTo(newValue);
        if (element == BlocklyEvent.ELEMENT_FIELD) {
            assertThat(deserializedEvent.getFieldName()).isEqualTo(FIELD_NAME);
        }
    }

    @Test
    public void testCreateEvent() throws JSONException, BlockLoadingException {
        BlocklyEvent.CreateEvent event = new BlocklyEvent.CreateEvent(mMockWorkspace, mBlock);

        assertThat(event.getTypeId()).isSameAs(BlocklyEvent.TYPE_CREATE);
        assertThat(event.getTypeName()).isSameAs(BlocklyEvent.TYPENAME_CREATE);
        assertThat(event.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        // Group id is assigned by the BlocklyController, not tested here.
        assertThat(event.getIds().size()).isEqualTo(1);
        assertThat(event.getIds().get(0)).isEqualTo(BLOCK_ID);

        String serialized = event.toJsonString();
        JSONObject json = new JSONObject(serialized);

        BlocklyEvent.CreateEvent deserializedEvent =
                (BlocklyEvent.CreateEvent) BlocklyEvent.fromJson(json);
        assertThat(deserializedEvent.getTypeName()).isEqualTo(event.getTypeName());
        // Workspace ids are not serialized.
        // Group id is assigned by the BlocklyController, not tested here.
        assertThat(deserializedEvent.getBlockId()).isEqualTo(BLOCK_ID);
        assertThat(deserializedEvent.getIds().size()).isEqualTo(1);
        assertThat(deserializedEvent.getIds().get(0)).isEqualTo(BLOCK_ID);

        mBlockFactory.clearPriorBlockReferences(); // Prevent duplicate block id errors.
        Block deserializedBlock =
                BlocklyXmlHelper.loadOneBlockFromXml(deserializedEvent.getXml(), mBlockFactory);
        assertThat(deserializedBlock.getId()).isEqualTo(BLOCK_ID);
        assertThat(mBlock.getType()).isEqualTo(BLOCK_TYPE);
        assertThat(mBlock.getPosition()).isEqualTo(NEW_POSITION);
    }
}
