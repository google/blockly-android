package com.google.blockly.model;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for {@link BlocklyEvent} classes.
 */
public class BlocklyEventTest extends BlocklyTestCase {
    private static final String BLOCK_TYPE = "controls_whileUntil";
    private static final String BLOCK_ID = "block";
    private static final String FIELD_NAME = "MODE";
    private static final String WORKSPACE_ID = "workspace";

    private static final int NEW_POSITION_X = 123;
    private static final int NEW_POSITION_Y = 456;
    private static final WorkspacePoint NEW_POSITION =
            new WorkspacePoint(NEW_POSITION_X, NEW_POSITION_Y);

    private BlocklyController mMockController;
    private BlockFactory mBlockFactory;
    private Block mBlock;
    private Field mField;

    @Before
    public void setUp() throws Exception {
        configureForUIThread();

        mMockController = Mockito.mock(BlocklyController.class);

        mBlockFactory = new BlockFactory();
        mBlockFactory.addJsonDefinitions(getContext().getAssets()
                .open("default/test_blocks.json"));
        mBlockFactory.setController(mMockController);
        mBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType(BLOCK_TYPE).withId(BLOCK_ID));
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mBlock.setEventWorkspaceId(WORKSPACE_ID);
            }
        });

        mField = mBlock.getFieldByName(FIELD_NAME);
        mBlock.setPosition(NEW_POSITION_X, NEW_POSITION_Y);
    }

    @Test
    public void testChangeEvent_commentText() throws JSONException {
        String oldValue = "old comment";
        String newValue = "new comment";
        BlocklyEvent.ChangeEvent event = BlocklyEvent.ChangeEvent.newCommentTextEvent(
                mBlock, oldValue, newValue);
        testChangeEvent(BlocklyEvent.ELEMENT_COMMENT, event, oldValue, newValue);
    }

    @Test
    public void testChangeEvent_fieldValue() throws JSONException {
        String oldValue = "UNTIL";
        String newValue = "WHILE";
        mField.setFromString(newValue);
        BlocklyEvent.ChangeEvent event = BlocklyEvent.ChangeEvent.newFieldValueEvent(
                mBlock, mField, oldValue, newValue);
        testChangeEvent(BlocklyEvent.ELEMENT_FIELD, event, oldValue, newValue);
    }

    @Test
    public void testChangeEvent_inlineState() throws JSONException {
        boolean newValue = mBlock.getInputsInline();
        boolean oldValue = !newValue;
        BlocklyEvent.ChangeEvent event =
                BlocklyEvent.ChangeEvent.newInlineStateEvent(mBlock);
        testChangeEvent(BlocklyEvent.ELEMENT_INLINE, event,
                oldValue ? "true" : "false",
                newValue ? "true" : "false");
    }

    @Test
    public void testChangeEvent_mutation() throws JSONException {
        String oldValue = "old mutation";
        String newValue = "new mutation";
        BlocklyEvent.ChangeEvent event = BlocklyEvent.ChangeEvent.newMutateEvent(
                mBlock, oldValue, newValue);
        testChangeEvent(BlocklyEvent.ELEMENT_MUTATE, event, oldValue, newValue);
    }

    private void testChangeEvent(@BlocklyEvent.ChangeElement String element,
                                 BlocklyEvent.ChangeEvent event,
                                 String oldValue, String newValue)
            throws JSONException {
        assertThat(event.getTypeId()).isSameAs(BlocklyEvent.TYPE_CHANGE);
        assertThat(event.getTypeName()).isSameAs(BlocklyEvent.TYPENAME_CHANGE);
        assertThat(event.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        // TODO(567): Assign the Block's parent/root EventWorkspace, and check against WORKSPACE_ID.
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
        BlocklyEvent.CreateEvent event = new BlocklyEvent.CreateEvent(mBlock);

        assertThat(event.getTypeId()).isSameAs(BlocklyEvent.TYPE_CREATE);
        assertThat(event.getTypeName()).isSameAs(BlocklyEvent.TYPENAME_CREATE);
        // TODO(567): Assign the Block's parent/root EventWorkspace, and check against WORKSPACE_ID.
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

// TODO
//        mBlockFactory.clearWorkspaceBlockReferences(); // Prevent duplicate block id errors.
//        Block deserializedBlock =
//                BlocklyXmlHelper.loadOneBlockFromXml(deserializedEvent.getXml(), mBlockFactory);
//        assertThat(deserializedBlock.getId()).isEqualTo(BLOCK_ID);
//        assertThat(mBlock.getType()).isEqualTo(BLOCK_TYPE);
//
//        // PointF.equals(other) did not exist before API 17. Compare components for 16.
//        WorkspacePoint position = mBlock.getPosition();
//        assertThat(position.x).isEqualTo(NEW_POSITION.x);
//        assertThat(position.y).isEqualTo(NEW_POSITION.y);
    }
}
