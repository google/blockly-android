package com.google.blockly.android.control;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.SparseArray;

import com.google.blockly.model.Block;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.WorkspacePoint;
import com.google.blockly.utils.BlocklyXmlHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class for all Blockly events.
 */
public abstract class BlocklyEvent {
    // JSON serialization attributes.  See also TYPENAME_ and ELEMENT_ constants for ids.
    private static final String JSON_BLOCK_ID = "blockId";
    private static final String JSON_ELEMENT = "element";
    private static final String JSON_GROUP_ID = "groupId";
    private static final String JSON_IDS = "ids";
    private static final String JSON_NAME = "name";
    private static final String JSON_NEW_VALUE = "newValue";
    private static final String JSON_OLD_VALUE = "oldValue";  // Rarely used.
    private static final String JSON_TYPE = "type";
    private static final String JSON_WORKSPACE_ID = "workspaceId"; // Rarely used.
    private static final String JSON_XML = "xml";

    @IntDef(flag = true,
            value = {TYPE_CHANGE, TYPE_CREATE, TYPE_DELETE, TYPE_MOVE, TYPE_UI})
    @Retention(RetentionPolicy.SOURCE)
    public @interface EventType {}

    public static final int TYPE_CREATE = 1 << 0;
    public static final int TYPE_DELETE = 1 << 1;
    public static final int TYPE_CHANGE = 1 << 2;
    public static final int TYPE_MOVE = 1 << 3;
    public static final int TYPE_UI = 1 << 4;
    // When adding an event type, update TYPE_ID_COUNT, TYPE_ALL, TYPE_ID_TO_NAME, and
    // TYPE_NAME_TO_ID.
    private static final int TYPE_ID_COUNT = 5;
    public static final @EventType int TYPE_ALL =
            TYPE_CHANGE | TYPE_CREATE | TYPE_DELETE | TYPE_MOVE | TYPE_UI;

    public static final String TYPENAME_CHANGE = "change";
    public static final String TYPENAME_CREATE = "create";
    public static final String TYPENAME_DELETE = "delete";
    public static final String TYPENAME_MOVE = "move";
    public static final String TYPENAME_UI = "ui";

    @StringDef({ELEMENT_COLLAPSED, ELEMENT_COMMENT, ELEMENT_DISABLED, ELEMENT_FIELD, ELEMENT_INLINE,
            ELEMENT_MUTATE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ChangeElement {}

    public static final String ELEMENT_COLLAPSED = "collapsed";
    public static final String ELEMENT_COMMENT = "comment";
    public static final String ELEMENT_DISABLED = "disabled";
    public static final String ELEMENT_FIELD = "field";
    public static final String ELEMENT_INLINE = "inline";
    public static final String ELEMENT_MUTATE = "mutate";

    @StringDef({ELEMENT_CATEGORY, ELEMENT_CLICK, ELEMENT_COMMENT_OPEN, ELEMENT_MUTATOR_OPEN,
            ELEMENT_SELECTED, ELEMENT_TRASH, ELEMENT_WARNING_OPEN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface UIElement {}

    public static final String ELEMENT_CATEGORY = "category";
    public static final String ELEMENT_CLICK = "click";
    public static final String ELEMENT_COMMENT_OPEN = "commentOpen";
    public static final String ELEMENT_MUTATOR_OPEN = "mutatorOpen";
    public static final String ELEMENT_SELECTED = "selected";
    public static final String ELEMENT_TRASH = "trashOpen";
    public static final String ELEMENT_WARNING_OPEN = "warningOpen";

    private static final SparseArray<String> TYPE_ID_TO_NAME = new SparseArray<>(TYPE_ID_COUNT);
    private static final Map<String, Integer> TYPE_NAME_TO_ID = new ArrayMap<>(TYPE_ID_COUNT);
    static {
        TYPE_ID_TO_NAME.put(TYPE_CHANGE, TYPENAME_CHANGE);
        TYPE_ID_TO_NAME.put(TYPE_CREATE, TYPENAME_CREATE);
        TYPE_ID_TO_NAME.put(TYPE_DELETE, TYPENAME_DELETE);
        TYPE_ID_TO_NAME.put(TYPE_MOVE, TYPENAME_MOVE);
        TYPE_ID_TO_NAME.put(TYPE_UI, TYPENAME_UI);

        TYPE_NAME_TO_ID.put(TYPENAME_CHANGE, TYPE_CHANGE);
        TYPE_NAME_TO_ID.put(TYPENAME_CREATE, TYPE_CREATE);
        TYPE_NAME_TO_ID.put(TYPENAME_DELETE, TYPE_DELETE);
        TYPE_NAME_TO_ID.put(TYPENAME_MOVE, TYPE_MOVE);
        TYPE_NAME_TO_ID.put(TYPENAME_UI, TYPE_UI);
    }

    public static BlocklyEvent fromJson(String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    public static BlocklyEvent fromJson(JSONObject json) throws JSONException {
        String typename = json.getString(JSON_TYPE);
        switch(typename) {
            case TYPENAME_CHANGE:
                return new ChangeEvent(json);
            case TYPENAME_CREATE:
                return new CreateEvent(json);
            case TYPENAME_DELETE:
                return new DeleteEvent(json);
            case TYPENAME_MOVE:
                return new MoveEvent(json);
            case TYPENAME_UI:
                return new UIEvent(json);

            default:
                throw new JSONException("Unknown event type: " + typename);
        }
    }

    @EventType
    protected final int mTypeId;
    protected final String mBlockId;
    protected final String mWorkspaceId;

    protected String mGroupId;

    /**
     * Base constructor for all BlocklyEvents.
     *
     * @param typeId The {@link EventType}.
     * @param workspaceId The id string of the Blockly workspace.
     * @param groupId The id string of the event group. Usually null for local events (assigned
     *                later); non-null for remote events.
     * @param blockId The id string of the block affected. Null for a few event types (e.g., toolbox
     *                category).
     */
    protected BlocklyEvent(@EventType int typeId, @NonNull String workspaceId,
                           @Nullable String groupId, @Nullable String blockId) {
        validateEventType(typeId);
        mTypeId = typeId;
        mBlockId = blockId;
        mWorkspaceId = workspaceId;
        mGroupId = groupId;
    }

    /**
     * Constructs BlocklyEvent with base attributes assigned from {@code json}.
     *
     * @param typeId The type of the event. Assumed to match {@link #JSON_TYPE} in {@code json}.
     * @param json The JSON object with event attribute values.
     * @throws JSONException
     */
    protected BlocklyEvent(@EventType int typeId, JSONObject json) throws JSONException {
        validateEventType(typeId);
        mTypeId = typeId;
        mWorkspaceId = json.optString(JSON_WORKSPACE_ID);
        mGroupId = json.optString(JSON_GROUP_ID);
        mBlockId = json.optString(JSON_BLOCK_ID);
    }

    /**
     * @return The type identifier for this event.
     */
    @EventType
    public int getTypeId() {
        return mTypeId;
    }

    /**
     * @return The JSON type identifier string for this event.
     */
    @NonNull
    public String getTypeName() {
        return TYPE_ID_TO_NAME.get(mTypeId);
    }

    /**
     * @return The identifier for the workspace that triggered this event.
     */
    @Nullable
    public String getWorkspaceId() {
        return mWorkspaceId;
    }

    /**
     * @return The identifier for the group of related events.
     */
    @Nullable
    public String getGroupId() {
        return mGroupId;
    }

    /**
     * @return The id of the primary or root affected block.
     */
    @Nullable
    public String getBlockId() {
        return mBlockId;
    }

    public String toJsonString() throws JSONException {
        JSONStringer out = new JSONStringer();
        out.object();
        out.key(JSON_TYPE);
        out.value(getTypeName());
        if (!TextUtils.isEmpty(mBlockId)) {
            out.key(JSON_BLOCK_ID);
            out.value(mBlockId);
        }
        if (!TextUtils.isEmpty(mGroupId)) {
            out.key(JSON_GROUP_ID);
            out.value(mGroupId);
        }
        writeJsonAttributes(out);
        // Workspace id is not included to reduce size over network.
        out.endObject();
        return out.toString();
    }

    protected void setGroupId(String groupId) {
        this.mGroupId = groupId;
    }

    protected abstract void writeJsonAttributes(JSONStringer out) throws JSONException;

    /**
     * Event fired when a property of a block changes.
     */
    public static final class ChangeEvent extends BlocklyEvent {
        /**
         * Creates a ChangeEvent reflecting a change in the block's collapsed state.
         *
         * @param workspace The workspace containing the block.
         * @param block The block where the state changed.
         * @return The new ChangeEvent.
         */
        public static ChangeEvent newCollapsedStateEvent(@NonNull Workspace workspace,
                                                         @NonNull Block block) {
            boolean collapsed = block.isCollapsed();
            return new ChangeEvent(ELEMENT_COLLAPSED, workspace, block, null,
                    /* oldValue */ !collapsed ? "true" : "false",
                    /* newValue */ collapsed ? "true" : "false");
        }

        /**
         * Creates a ChangeEvent reflecting a change in the block's comment text.
         *
         * @param workspace The workspace containing the block.
         * @param block The block where the state changed.
         * @param oldValue The prior comment text.
         * @param newValue The updated comment text.
         * @return The new ChangeEvent.
         */
        public static ChangeEvent newCommentTextEvent(
                @NonNull Workspace workspace, @NonNull Block block,
                @Nullable String oldValue, @Nullable String newValue) {
            return new ChangeEvent(ELEMENT_COMMENT, workspace, block, null, oldValue, newValue);
        }

        /**
         * Creates a ChangeEvent reflecting a change in the block's disabled state.
         *
         * @param workspace The workspace containing the block.
         * @param block The block where the state changed.
         * @return The new ChangeEvent.
         */
        public static ChangeEvent newDisabledStateEvent(@NonNull Workspace workspace,
                                                        @NonNull Block block) {
            boolean disabled = block.isDisabled();
            return new ChangeEvent(ELEMENT_DISABLED, workspace, block, null,
                    /* oldValue */ !disabled ? "true" : "false",
                    /* newValue */ disabled ? "true" : "false");
        }

        /**
         * Creates a ChangeEvent reflecting a change in a field's value.
         *
         * @param workspace The workspace containing the block.
         * @param block The block where the state changed.
         * @param field The field with the changed value.
         * @param oldValue The prior value.
         * @param newValue The updated value.
         * @return The new ChangeEvent.
         */
        public static ChangeEvent newFieldValueEvent(
                @NonNull Workspace workspace, @NonNull Block block, @NonNull Field field,
                @NonNull String oldValue, @NonNull String newValue) {
            return new ChangeEvent(ELEMENT_FIELD, workspace, block, field, oldValue, newValue);
        }

        /**
         * Creates a ChangeEvent reflecting a change in the block's inlined inputs state.
         *
         * @param workspace The workspace containing the block.
         * @param block The block where the state changed.
         * @return The new ChangeEvent.
         */
        public static ChangeEvent newInlineStateEvent(@NonNull Workspace workspace,
                                                      @NonNull Block block) {
            boolean inline = block.getInputsInline();
            return new ChangeEvent(ELEMENT_INLINE, workspace, block, null,
                    /* oldValue */ !inline ? "true" : "false",
                    /* newValue */ inline ? "true" : "false");
        }

        /**
         * Creates a ChangeEvent reflecting a change in the block's mutation state.
         *
         * @param workspace The workspace containing the block.
         * @param block The block where the state changed.
         * @param oldValue The serialized version of the prior mutation state.
         * @param newValue The serialized version of the updated mutation state.
         * @return The new ChangeEvent.
         */
        public static ChangeEvent newMutateEvent(
                @NonNull Workspace workspace, @NonNull Block block,
                @Nullable String oldValue, @Nullable String newValue) {
            return new ChangeEvent(ELEMENT_MUTATE, workspace, block, null, oldValue, newValue);
        }

        @NonNull @ChangeElement
        private final String mElementChanged;
        @Nullable
        private final String mFieldName;
        @NonNull
        private final String mOldValue;
        @NonNull
        private final String mNewValue;

        /**
         * Constructs a ChangeEvent, signifying {@code block}'s value changed.
         *
         * @param workspace The workspace containing the change.
         * @param block The block containing the change.
         * @param field The field containing the change, if the change is a field value. Otherwise
         *              null.
         * @param oldValue The original value.
         * @param newValue The new value.
         */
        private ChangeEvent(@ChangeElement String element, @NonNull Workspace workspace,
                            @NonNull Block block, @Nullable Field field,
                            @Nullable String oldValue, @Nullable String newValue) {
            super(TYPE_CHANGE, workspace.getId(), null, block.getId());
            mElementChanged = validateChangeElement(element);
            if (mElementChanged == ELEMENT_FIELD) {
                mFieldName = field.getName();
            }  else {
                mFieldName = null; // otherwise ignore the field name
            }
            mOldValue = oldValue;
            mNewValue = newValue;
        }

        /**
         * Constructs a ChangeEvent from the JSON serialized representation.
         *
         * @param json The serialized ChangeEvent.
         * @throws JSONException
         */
        public ChangeEvent(@NonNull JSONObject json) throws JSONException {
            super(TYPE_CHANGE, json);
            if (TextUtils.isEmpty(mBlockId)) {
                throw new JSONException(JSON_BLOCK_ID + " must be assigned.");
            }
            String element = json.getString(JSON_ELEMENT);
            try {
                mElementChanged = validateChangeElement(element);
            } catch (IllegalArgumentException e) {
                throw new JSONException("Invalid change element: " + element);
            }
            mFieldName = (mElementChanged == ELEMENT_FIELD) ? json.getString(JSON_NAME) : null;
            mOldValue = json.optString(JSON_OLD_VALUE); // Not usually serialized.
            mNewValue = json.getString(JSON_NEW_VALUE);
        }

        @NonNull @ChangeElement
        public String getElement() {
            return mElementChanged;
        }

        public String getFieldName() {
            return mFieldName;
        }

        public String getOldValue() {
            return mOldValue;
        }

        public String getNewValue() {
            return mNewValue;
        }

        protected void writeJsonAttributes(JSONStringer out) throws JSONException {
            out.key("element");
            out.value(mElementChanged);
            if (mFieldName != null) {
                out.key("name");
                out.value(mFieldName);
            }
            out.key("newValue");
            out.value(mNewValue);
        }
    }

    /**
     * Event fired when a block is added to the workspace, possibly containing other child blocks
     * and next blocks.
     */
    public static final class CreateEvent extends BlocklyEvent {
        private final String mXml;
        private final List<String> mIds;

        /**
         * Constructs a {@code CreateEvent} for the given block.
         *
         * @param workspace The workspace containing the new block.
         * @param block The newly created block.
         */
        public CreateEvent(@NonNull Workspace workspace, @NonNull Block block) {
            super(TYPE_CREATE, workspace.getId(), null, block.getId());
            try {
                mXml = BlocklyXmlHelper.writeOneBlockToXml(block);
            } catch (BlocklySerializerException e) {
                throw new IllegalArgumentException("Invalid block for event serialization");
            }

            List<String> ids = new ArrayList<>();
            block.addAllBlockIds(ids);
            mIds = Collections.unmodifiableList(ids);
        }

        /**
         * Constructs a CreateEvent from the JSON serialized representation.
         *
         * @param json The serialized CreateEvent.
         * @throws JSONException
         */
        public CreateEvent(JSONObject json) throws JSONException {
            super(TYPE_CREATE, json);
            if (mBlockId == null) {
                throw new JSONException(JSON_BLOCK_ID + " must be assigned.");
            }
            mXml = json.getString(JSON_XML);

            JSONArray jsonIds = json.getJSONArray("ids");
            int count = jsonIds.length();
            List<String> ids = new ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                ids.add(jsonIds.getString(i));
            }
            mIds = Collections.unmodifiableList(ids);
        }

        /**
         * @return The XML serialization of all blocks created by this event.
         */
        public String getXml() {
            return mXml;
        }

        /**
         * @return The list of all block ids for all blocks created by this event.
         */
        public List<String> getIds() {
            return mIds;
        }

        @Override
        protected void writeJsonAttributes(JSONStringer out) throws JSONException {
            out.key("xml");
            out.value(mXml);
            out.key("ids");
            out.array();
            for (String id : mIds) {
                out.value(id);
            }
            out.endArray();
        }
    }

    /**
     * Event fired when a block is removed from the workspace.
     */
    public static final class DeleteEvent extends BlocklyEvent {
        private final String mOldXml;
        private final List<String> mIds;

        /**
         * Constructs a {@code DeleteEvent}, signifying the removal of a block from the workspace.
         *
         * @param workspace The workspace containing the deletion.
         * @param block The deleted block (or to-be-deleted block), with all children attached.
         */
        DeleteEvent(@NonNull Workspace workspace, @NonNull Block block) {
            super(TYPE_DELETE, workspace.getId(), null, block.getId());
            try {
                mOldXml = BlocklyXmlHelper.writeOneBlockToXml(block);
            } catch (BlocklySerializerException e) {
                throw new IllegalArgumentException("Invalid block for event serialization");
            }

            List<String> ids = new ArrayList<>();
            block.addAllBlockIds(ids);
            mIds = Collections.unmodifiableList(ids);
        }

        /**
         * Constructs a DeleteEvent from the JSON serialized representation.
         *
         * @param json The serialized DeleteEvent.
         * @throws JSONException
         */
        DeleteEvent(@NonNull JSONObject json) throws JSONException {
            super(TYPE_DELETE, json);
            if (TextUtils.isEmpty(mBlockId)) {
                throw new JSONException(TYPENAME_DELETE + " requires " + JSON_BLOCK_ID);
            }

            mOldXml = json.optString(JSON_OLD_VALUE); // Not usually used.
            JSONArray ids = json.getJSONArray(JSON_IDS);
            int count = ids.length();
            List<String> temp = new ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                temp.add(ids.getString(i));
            }
            mIds = Collections.unmodifiableList(temp);
        }

        /**
         * @return The XML serialization of all blocks deleted by this event.
         */
        public String getXml() {
            return mOldXml;
        }

        /**
         * @return The list of all block ids for all blocks deleted by this event.
         */
        public List<String> getIds() {
            return mIds;
        }

        @Override
        protected void writeJsonAttributes(JSONStringer out) throws JSONException {
            out.key("ids");
            out.array();
            for (String id : mIds) {
                out.value(id);
            }
            out.endArray();
        }
    }

    /**
     * Event fired when a block is moved on the workspace, or its parent connection is changed.
     * <p/>
     * This event must be created before the block is moved to capture the original position.
     * After the move has been completed in the workspace, capture the updated position or parent
     * using {@link #recordNew(Block)}.  All of this is managed by {@link BlocklyController}, before
     * {@link BlocklyController.EventsCallback}s receive the event.
     */
    public static final class MoveEvent extends BlocklyEvent {
        private static final String JSON_NEW_COORDINATE = "newCoordinate";
        private static final String JSON_NEW_INPUT_NAME = "newInputName";
        private static final String JSON_NEW_PARENT_ID = "newParentId";

        @Nullable
        private String mOldParentId;
        @Nullable
        private String mOldInputName;
        private boolean mHasOldPosition;
        private int mOldPositionX;
        private int mOldPositionY;

        // New values are recorded
        @Nullable
        private String mNewParentId;
        @Nullable
        private String mNewInputName;
        private boolean mHasNewPosition;
        private int mNewPositionX;
        private int mNewPositionY;

        /**
         * Constructs a {@link MoveEvent} signifying the movement of a block on the workspace.
         *
         * @param workspace The workspace containing the moved blocks.
         * @param block The root block of the move, while it is still in its original position.
         */
        MoveEvent(@NonNull Workspace workspace, @NonNull Block block) {
            super(TYPE_MOVE, workspace.getId(), null, block.getId());

            Connection parentConnection = block.getParentConnection();
            if (parentConnection == null) {
                WorkspacePoint position = block.getPosition();
                if (position == null) {
                    throw new IllegalStateException("Block must have parent or position.");
                }
                mHasOldPosition = true;
                mOldPositionX = position.x;
                mOldPositionY = position.y;
                mOldParentId = null;
                mOldInputName = null;
            } else {
                Input parentInput = parentConnection.getInput();
                mOldParentId = parentConnection.getBlock().getId();
                mOldInputName = parentInput == null ? null : parentInput.getName();
                mHasOldPosition = false;
                mOldPositionX = mOldPositionY = -1;
            }
        }

        /**
         * Constructs a MoveEvent from the JSON serialized representation.
         *
         * @param json The serialized MoveEvent.
         * @throws JSONException
         */
        MoveEvent(JSONObject json) throws JSONException {
            super(TYPE_MOVE, json);
            if (TextUtils.isEmpty(mBlockId)) {
                throw new JSONException(TYPENAME_MOVE + " requires " + JSON_BLOCK_ID);
            }

            // Old values are not stored in JSON
            mOldParentId = null;
            mOldInputName = null;
            mOldPositionX = mOldPositionY = 0;

            String newCoordinateStr = json.optString(JSON_NEW_COORDINATE);
            if (newCoordinateStr != null) {
                // JSON coordinates are always integers, separated by a comma.
                int comma = newCoordinateStr.indexOf(',');
                if (comma == -1) {
                    throw new JSONException(
                            "Invalid " + JSON_NEW_COORDINATE + ": " + newCoordinateStr);
                }
                try {
                    mNewPositionX = Integer.parseInt(newCoordinateStr.substring(0, comma));
                    mNewPositionY = Integer.parseInt(newCoordinateStr.substring(comma + 1));
                } catch (NumberFormatException e) {
                    throw new JSONException(
                            "Invalid " + JSON_NEW_COORDINATE + ": " + newCoordinateStr);
                }
            } else {

            }
        }

        public void recordNew(Block block) {
            if (!block.getId().equals(mBlockId)) {
                throw new IllegalArgumentException("Block id does not match original.");
            }

            Connection parentConnection = block.getParentConnection();
            if (parentConnection == null) {
                WorkspacePoint position = block.getPosition();
                if (position == null) {
                    throw new IllegalStateException("Block must have parent or position.");
                }
                mHasNewPosition = true;
                mNewPositionX = position.x;
                mNewPositionY = position.y;
                mNewParentId = null;
                mNewInputName = null;
            } else {
                mNewParentId = parentConnection.getBlock().getId();
                if (parentConnection.getType() == Connection.CONNECTION_TYPE_NEXT) {
                    mNewInputName = null;
                } else {
                    mNewInputName = parentConnection.getInput().getName();
                }
                mHasNewPosition = false;
                mNewPositionX = mNewPositionY = -1;
            }
        }

        public String getOldParentId() {
            return mOldParentId;
        }

        public String getOldInputName() {
            return mOldInputName;
        }

        public boolean hasOldPosition() {
            return mHasOldPosition;
        }

        public boolean getOldWorkspacePosition(WorkspacePoint output) {
            if (mHasOldPosition) {
                output.set(mOldPositionX, mOldPositionY);
            }
            return mHasOldPosition;
        }

        public String getNewParentId() {
            return mNewParentId;
        }

        public String getNewInputName() {
            return mNewInputName;
        }

        public boolean hasNewPosition() {
            return mHasNewPosition;
        }

        public boolean getNewWorkspacePosition(WorkspacePoint output) {
            if (mHasNewPosition) {
                output.set(mNewPositionX, mNewPositionY);
            }
            return mHasNewPosition;
        }

        @Override
        protected void writeJsonAttributes(JSONStringer out) throws JSONException {
            if (mNewParentId != null) {
                out.key(JSON_NEW_PARENT_ID);
                out.value(mNewParentId);
            }
            if (mNewInputName != null) {
                out.key(JSON_NEW_INPUT_NAME);
                out.value(mNewInputName);
            }
            if (mHasNewPosition) {
                out.key(JSON_NEW_COORDINATE);
                StringBuilder sb = new StringBuilder();
                sb.append(mNewPositionX).append(',').append(mNewPositionY);
                out.value(sb.toString());
            }
        }
    }

    /**
     * Event class for user interface related actions, including selecting blocks, opening/closing
     * the toolbox or trash, and changing toolbox categories.
     */
    public static final class UIEvent extends BlocklyEvent {
        private final @BlocklyEvent.UIElement String mUiElement;
        private final String mOldValue;
        private final String mNewValue;

        public UIEvent newBlockClickedEvent(@NonNull Workspace workspace, @NonNull Block block) {
            return new UIEvent(ELEMENT_CLICK, workspace, block, null, null);
        }

        public UIEvent newBlockCommentEvent(@NonNull Workspace workspace, @NonNull Block block,
                                            boolean openedBefore, boolean openedAfter) {
            return new UIEvent(ELEMENT_COMMENT_OPEN, workspace, block,
                    openedBefore ? "true" : "false", openedAfter ? "true" : "false");
        }

        public UIEvent newBlockMutatorEvent(@NonNull Workspace workspace, @NonNull Block block,
                                            boolean openedBefore, boolean openedAfter) {
            return new UIEvent(ELEMENT_MUTATOR_OPEN, workspace, block,
                    openedBefore ? "true" : "false", openedAfter ? "true" : "false");
        }

        public UIEvent newBlockSelectedEvent(@NonNull Workspace workspace, @NonNull Block block,
                                            boolean selectedBefore, boolean selectedAfter) {
            return new UIEvent(ELEMENT_SELECTED, workspace, block,
                               selectedBefore ? "true" : "false", selectedAfter ? "true" : "false");
        }

        public UIEvent newBlockWarningEvent(@NonNull Workspace workspace, @NonNull Block block,
                                            boolean openedBefore, boolean openedAfter) {
            return new UIEvent(ELEMENT_WARNING_OPEN, workspace, block,
                    openedBefore ? "true" : "false", openedAfter ? "true" : "false");
        }

        public UIEvent newToolboxCategoryEvent(@NonNull Workspace workspace,
                                               @Nullable String oldValue,
                                               @Nullable String newValue) {
            return new UIEvent(ELEMENT_CATEGORY, workspace, null, oldValue, newValue);
        }

        /**
         * Constructs a block related UI event, such as clicked, selected, comment opened, mutator
         * opened, or warning opened.
         *
         * @param element The UI element that changed.
         * @param workspace The workspace containing the moved blocks.
         * @param block The related block. Null for toolbox category events.
         * @param oldValue The value before the event. Booleans are mapped to "true" and "false".
         * @param newValue The value after the event. Booleans are mapped to "true" and "false".
         */
        private UIEvent(@BlocklyEvent.UIElement String element, @NonNull Workspace workspace,
                        @Nullable Block block, String oldValue, String newValue) {
            super(TYPE_UI, workspace.getId(), null, block == null ? null : block.getId());
            this.mUiElement = validateUiElement(element);
            this.mOldValue = oldValue;
            this.mNewValue = newValue;
        }

        /**
         * Constructs a UIEvent from the JSON serialized representation.
         *
         * @param json The serialized UIEvent.
         * @throws JSONException
         */
        UIEvent(JSONObject json) throws JSONException {
            super(TYPE_UI, json);
            String element = json.getString(JSON_ELEMENT);
            try {
                mUiElement = validateUiElement(element);
            } catch (IllegalArgumentException e) {
                throw new JSONException("Invalid UI element: " + element);
            }
            if (mUiElement != ELEMENT_CATEGORY && TextUtils.isEmpty(mBlockId)) {
                throw new JSONException("UI element " + mUiElement + " requires " + JSON_BLOCK_ID);
            }
            this.mOldValue = json.optString(JSON_OLD_VALUE);  // Rarely used.
            this.mNewValue = json.optString(JSON_NEW_VALUE);
            if (mUiElement != ELEMENT_CATEGORY && mUiElement != ELEMENT_CLICK
                    && TextUtils.isEmpty(mNewValue)) {
                throw new JSONException("UI element " + mUiElement + " requires " + JSON_NEW_VALUE);
            }
        }

        public String getElement() {
            return mUiElement;
        }

        public String getOldValue() {
            return mOldValue;
        }

        public String getNewValue() {
            return mNewValue;
        }

        @Override
        protected void writeJsonAttributes(JSONStringer out) throws JSONException {
            out.key("element");
            out.value(mUiElement);
            if (mNewValue != null) {
                out.key("newValue");
                out.value(mNewValue);
            }
            // Old value is not included to reduce size over network.
        }
    }


    /**
     * Ensures {@code typeId} is a singular valid event id.
     * @param typeId The typeId to test.
     */
    private static void validateEventType(int typeId) {
        if (typeId <= 0 || typeId > TYPE_ALL // Outside bounds.
                || ((typeId & (typeId - 1)) != 0)) /* Not a power of two */ {
            throw new IllegalArgumentException("Invalid typeId: " + typeId);
        }
    }

    /**
     * @param eventTypeName A JSON "type" event name.
     * @return returns The {@link EventType} for {@code eventTypeName}, or 0 if not valid.
     * @throws IllegalArgumentException when
     */
    private static int getIdForEventName(final String eventTypeName) {
        Integer typeId = TYPE_NAME_TO_ID.get(eventTypeName);
        if (typeId == null) {
            return 0;
        }
        return typeId;
    }

    /**
     * @param changeElement An element name string, as used by {@link ChangeEvent}s.
     * @return The canonical (identity comparable) version of {@code changeElement}.
     * @throws IllegalArgumentException If {@code changeElement} is not a {@link ChangeElement}.
     */
    private static String validateChangeElement(final String changeElement) {
        switch (changeElement) {
            case ELEMENT_COLLAPSED:
                return ELEMENT_COLLAPSED;
            case ELEMENT_COMMENT:
                return ELEMENT_COMMENT;
            case ELEMENT_DISABLED:
                return ELEMENT_DISABLED;
            case ELEMENT_FIELD:
                return ELEMENT_FIELD;
            case ELEMENT_INLINE:
                return ELEMENT_INLINE;
            case ELEMENT_MUTATE:
                return ELEMENT_MUTATE;

            default:
                throw new IllegalArgumentException("Unrecognized change element: " + changeElement);
        }
    }

    /**
     * @param uiElement An element name string, as used by {@link UIEvent}s.
     * @return The canonical (identity comparable) version of the {@code uiElement}.
     * @throws IllegalArgumentException If {@code changeElement} is not a {@link UIElement}.
     */
    private static String validateUiElement(final String uiElement) {
        switch (uiElement) {
            case ELEMENT_CATEGORY:
                return ELEMENT_CATEGORY;
            case ELEMENT_CLICK:
                return ELEMENT_CLICK;
            case ELEMENT_COMMENT_OPEN:
                return ELEMENT_COMMENT_OPEN;
            case ELEMENT_MUTATOR_OPEN:
                return ELEMENT_MUTATOR_OPEN;
            case ELEMENT_SELECTED:
                return ELEMENT_SELECTED;
            case ELEMENT_WARNING_OPEN:
                return ELEMENT_WARNING_OPEN;

            default:
                throw new IllegalArgumentException("Unrecognized UI element: " + uiElement);
        }
    }
}
