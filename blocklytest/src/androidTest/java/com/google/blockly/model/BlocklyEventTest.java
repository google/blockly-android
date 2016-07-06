package com.google.blockly.model;

import com.google.blockly.android.MockitoAndroidTestCase;

import com.google.blockly.android.control.BlocklyEvent;
import com.google.blockly.utils.BlocklyXmlHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * Unit tests for {@link BlocklyEvent} classes.
 */
public class BlocklyEventTest extends MockitoAndroidTestCase {
    private static final String BLOCK_TYPE = "controls_whileUntil";
    private static final String BLOCK_ID = "block";
    private static final String FIELD_NAME = "MODE";
    private static final String WORKSPACE_ID = "workspace";

    private static final int NEW_POSITION_X = 123;
    private static final int NEW_POSITION_Y = 456;
    private static final WorkspacePoint NEW_POSITION =
            new WorkspacePoint(NEW_POSITION_X, NEW_POSITION_Y);

    @Mock Workspace mMockWorkspace;

    BlockFactory mBlockFactory;
    Block mBlock;
    Field mField;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mBlockFactory = new BlockFactory(getContext(),
                new int[]{com.google.blockly.android.R.raw.test_blocks});
        mBlock = mBlockFactory.obtainBlock(BLOCK_TYPE, BLOCK_ID);
        mField = mBlock.getFieldByName(FIELD_NAME);
        mBlock.setPosition(NEW_POSITION_X, NEW_POSITION_Y);


        Mockito.when(mMockWorkspace.getId()).thenReturn(WORKSPACE_ID);
    }

    public void testChangeEvent_collapsedState() throws JSONException {
        boolean newValue = mBlock.isCollapsed();
        boolean oldValue = !newValue;
        BlocklyEvent.ChangeEvent event =
                BlocklyEvent.ChangeEvent.newCollapsedStateEvent(mMockWorkspace, mBlock);
        testChangeEvent(BlocklyEvent.ELEMENT_COLLAPSED, event,
                        oldValue ? "true" : "false",
                        newValue ? "true" : "false");
    }

    public void testChangeEvent_commentText() throws JSONException {
        String oldValue = "old comment";
        String newValue = "new comment";
        BlocklyEvent.ChangeEvent event = BlocklyEvent.ChangeEvent.newCommentTextEvent(
                mMockWorkspace, mBlock, oldValue, newValue);
        testChangeEvent(BlocklyEvent.ELEMENT_COMMENT, event, oldValue, newValue);
    }

    public void testChangeEvent_disabledState() throws JSONException {
        boolean newValue = mBlock.isDisabled();
        boolean oldValue = !newValue;
        BlocklyEvent.ChangeEvent event =
                BlocklyEvent.ChangeEvent.newDisabledStateEvent(mMockWorkspace, mBlock);
        testChangeEvent(BlocklyEvent.ELEMENT_DISABLED, event,
                oldValue ? "true" : "false",
                newValue ? "true" : "false");
    }

    public void testChangeEvent_fieldValue() throws JSONException {
        String oldValue = "UNTIL";
        String newValue = "WHILE";
        mField.setFromString(newValue);
        BlocklyEvent.ChangeEvent event = BlocklyEvent.ChangeEvent.newFieldValueEvent(
                mMockWorkspace, mBlock, mField, oldValue, newValue);
        testChangeEvent(BlocklyEvent.ELEMENT_FIELD, event, oldValue, newValue);
    }

    public void testChangeEvent_inlineState() throws JSONException {
        boolean newValue = mBlock.getInputsInline();
        boolean oldValue = !newValue;
        BlocklyEvent.ChangeEvent event =
                BlocklyEvent.ChangeEvent.newInlineStateEvent(mMockWorkspace, mBlock);
        testChangeEvent(BlocklyEvent.ELEMENT_INLINE, event,
                oldValue ? "true" : "false",
                newValue ? "true" : "false");
    }

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
        assertSame(event.getTypeId(), BlocklyEvent.TYPE_CHANGE);
        assertSame(event.getTypeName(), BlocklyEvent.TYPENAME_CHANGE);
        assertEquals(WORKSPACE_ID, event.getWorkspaceId());
        // Group id is assigned by the BlocklyController, not tested here.
        assertEquals(element, event.getElement());
        assertEquals(oldValue, event.getOldValue());
        assertEquals(newValue, event.getNewValue());
        if (element == BlocklyEvent.ELEMENT_FIELD) {
            assertEquals(FIELD_NAME, event.getFieldName());
        }

        String serialized = event.toJsonString();
        JSONObject json = new JSONObject(serialized);

        BlocklyEvent.ChangeEvent deserializedEvent =
                (BlocklyEvent.ChangeEvent) BlocklyEvent.fromJson(json);
        assertEquals(event.getTypeName(), deserializedEvent.getTypeName());
        // Workspace ids are not serialized.
        // Group id is assigned by the BlocklyController, not tested here.
        assertEquals(element, deserializedEvent.getElement());
        assertEquals(BLOCK_ID, deserializedEvent.getBlockId());
        assertEquals(newValue, deserializedEvent.getNewValue());
        if (element == BlocklyEvent.ELEMENT_FIELD) {
            assertEquals(FIELD_NAME, deserializedEvent.getFieldName());
        }
    }

    public void testCreateEvent() throws JSONException {
        BlocklyEvent.CreateEvent event = new BlocklyEvent.CreateEvent(mMockWorkspace, mBlock);

        assertSame(event.getTypeId(), BlocklyEvent.TYPE_CREATE);
        assertSame(event.getTypeName(), BlocklyEvent.TYPENAME_CREATE);
        assertEquals(WORKSPACE_ID, event.getWorkspaceId());
        // Group id is assigned by the BlocklyController, not tested here.
        assertEquals(1, event.getIds().size());
        assertEquals(BLOCK_ID, event.getIds().get(0));

        String serialized = event.toJsonString();
        JSONObject json = new JSONObject(serialized);

        BlocklyEvent.CreateEvent deserializedEvent =
                (BlocklyEvent.CreateEvent) BlocklyEvent.fromJson(json);
        assertEquals(event.getTypeName(), deserializedEvent.getTypeName());
        // Workspace ids are not serialized.
        // Group id is assigned by the BlocklyController, not tested here.
        assertEquals(BLOCK_ID, deserializedEvent.getBlockId());
        assertEquals(1, deserializedEvent.getIds().size());
        assertEquals(BLOCK_ID, deserializedEvent.getIds().get(0));

        mBlockFactory.clearPriorBlockReferences(); // Prevent duplicate block id errors.
        Block deserializedBlock =
                BlocklyXmlHelper.loadOneBlockFromXml(deserializedEvent.getXml(), mBlockFactory);
        assertEquals(BLOCK_ID, deserializedBlock.getId());
        assertEquals(BLOCK_TYPE, mBlock.getType());
        assertEquals(NEW_POSITION, mBlock.getPosition());
    }
}
