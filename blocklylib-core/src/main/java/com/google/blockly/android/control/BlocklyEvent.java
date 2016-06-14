package com.google.blockly.android.control;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.SparseArray;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Input;
import com.google.blockly.model.WorkspacePoint;

import org.json.JSONException;
import org.json.JSONStringer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class to all Blockly events.
 */
public abstract class BlocklyEvent {
    @IntDef(flag = true,
            value = {TYPE_CHANGE, TYPE_CREATE, TYPE_DELETE, TYPE_MOVE, TYPE_UI})
    @Retention(RetentionPolicy.SOURCE)
    public @interface EventType {}

    public static final int TYPE_CREATE = 1 << 0;
    public static final int TYPE_DELETE = 1 << 1;
    public static final int TYPE_CHANGE = 1 << 2;
    public static final int TYPE_MOVE = 1 << 3;
    public static final int TYPE_UI = 1 << 4;
    // When adding an event type, update TYPE_ID_COUNT, TYPE_ALL, TYPE_ID_TO_NAME, and TYPE_NAME_TO_ID.
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

    /**
     * Ensures {@code typeId} is a singular valid event it.
     * @param typeId The typeId to test.
     */
    public static void validateEventType(int typeId) {
        if (typeId <= 0 || typeId > TYPE_ALL // Outside bounds.
                || ((typeId & (typeId - 1)) != 0)) /* Not a power of two */ {
            throw new IllegalArgumentException("Invalid typeId: " + typeId);
        }
    }

    /**
     * @param eventTypeName A JSON "type" event name.
     * @return returns The {@link EventType} for {@code eventTypeName}.
     * @throws IllegalArgumentException when
     */
    public static int getIdForEventName(final String eventTypeName) {
        Integer typeId = TYPE_NAME_TO_ID.get(eventTypeName);
        if (typeId == null) {
            throw new IllegalArgumentException("Unrecognized event type: " + eventTypeName);
        }
        return typeId;
    }

    /**
     * @param changeElement An element name string, as used by {@link ChangeEvent}s.
     * @return The canonical (identity comparable) version of {@code changeElement}.
     * @throws IllegalArgumentException If {@code changeElement} is not a {@link ChangeElement}.
     */
    public static String validateChangeElement(final @ChangeElement String changeElement) {
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
    public static String validateUiElement(final @UIElement String uiElement) {
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

    @EventType
    protected final int mTypeId;
    protected final String mBlockId;
    protected final String mWorkspaceId;

    protected String mGroupId;

    protected BlocklyEvent(@EventType int typeId, @Nullable String blockId,
                           @Nullable String workspaceId, @Nullable String groupId) {
        validateEventType(typeId);
        mTypeId = typeId;
        mBlockId = blockId;
        mWorkspaceId = workspaceId;
        mGroupId = groupId;
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

    public String toJsonString() throws JSONException {
        JSONStringer out = new JSONStringer();
        out.object();
        out.key("type");
        out.value(getTypeName());
        if (!TextUtils.isEmpty(mBlockId)) {
            out.key("blockId");
            out.value(mBlockId);
        }
        if (!TextUtils.isEmpty(mGroupId)) {
            out.key("groupId");
            out.value(mGroupId);
        }
        writeJsonAttributes(out);
        out.endObject();
        return out.toString();
    }

    protected void setGroupId(String groupId) {
        this.mGroupId = groupId;
    }

    protected abstract void writeJsonAttributes(JSONStringer out) throws JSONException;

    /**
     * Event fired when a block's field value has changed.
     */
    public static class ChangeEvent extends BlocklyEvent {
        @NonNull @ChangeElement
        private final String mElementChanged;
        @Nullable
        private final String mFieldName;
        @NonNull
        private final String mOldValue;
        @NonNull
        private final String mNewValue;


        ChangeEvent(@NonNull String workspaceId, @Nullable String groupId, @NonNull String blockId,
                    @NonNull String fieldName, @NonNull @ChangeElement String elementChanged,
                    @NonNull String oldValue, @NonNull String newValue) {
            super(TYPE_CHANGE, workspaceId, groupId, blockId);
            if (TextUtils.isEmpty(workspaceId) || TextUtils.isEmpty(blockId)
                    || TextUtils.isEmpty(elementChanged)) {
                throw new IllegalArgumentException("The following must not be null or empty: "
                        + "workspaceId, blockId, or changeElemet");
            }
            if (oldValue == null || newValue == null) {
                throw new IllegalArgumentException("oldValue and newValue cannot be null");
            }
            mElementChanged = validateChangeElement(elementChanged);
            if (mElementChanged == ELEMENT_FIELD) {
                if (TextUtils.isEmpty(fieldName)) {
                    throw new IllegalArgumentException("field cannot be null");
                }
                mFieldName = fieldName;
            }  else {
                mFieldName = null; // otherwise ignore the field name
            }
            mOldValue = oldValue;
            mNewValue = newValue;
        }

        @NonNull @ChangeElement
        public String getElement() {
            return mElementChanged;
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
    public static class CreateEvent extends BlocklyEvent {
        private final String mXml;
        private final List<String> mIds;

        CreateEvent(@NonNull String workspaceId, @NonNull String groupId, @Nullable String blockId,
                    @NonNull String xml, @NonNull List<String> ids) {
            super(TYPE_CREATE, workspaceId, groupId, blockId);
            this.mXml = xml;
            this.mIds = Collections.unmodifiableList(ids);
        }

        public String getXml() {
            return mXml;
        }

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
    public static class DeleteEvent extends BlocklyEvent {
        private final String mOldXml;
        private final List<String> mIds;

        DeleteEvent(String workspaceId, String groupId, @Nullable String blockId, String oldXml, List<String> ids) {
            super(TYPE_DELETE, workspaceId, groupId, blockId);
            this.mOldXml = oldXml;
            this.mIds = Collections.unmodifiableList(ids);
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
     */
    public static class MoveEvent extends BlocklyEvent {
        @Nullable
        private final String mOldParentId;
        @Nullable
        private final String mOldInputName;
        private final boolean mHasOldPosition;
        private final int mOldPositionX;
        private final int mOldPositionY;

        // New values are recorded
        @Nullable
        private String mNewParentId;
        @Nullable
        private String mNewInputName;
        private boolean mHasNewPosition;
        private int mNewPositionX;
        private int mNewPositionY;

        /**
         * Creates a new
         * @param workspaceId
         * @param groupId
         * @param block
         */
        MoveEvent(String workspaceId, String groupId, @NonNull Block block) {
            super(TYPE_MOVE, workspaceId, groupId, block.getId());

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
                mOldInputName = parentInput.getName();
                mOldParentId = parentInput.getBlock().getId();
                mHasOldPosition = false;
                mOldPositionX = mOldPositionY = -1;
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
                Input parentInput = parentConnection.getInput();
                mNewInputName = parentInput.getName();
                mNewParentId = parentInput.getBlock().getId();
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

        public WorkspacePoint getOldWorkspacePosition(WorkspacePoint output) {
            output.set(mOldPositionX, mOldPositionY);
            return output;
        }

        public String getNewParentId() {
            return mNewParentId;
        }

        public String getNewInputName() {
            return mNewInputName;
        }

        public WorkspacePoint getNewWorkspacePosition(WorkspacePoint output) {
            output.set(mNewPositionX, mNewPositionY);
            return output;
        }

        @Override
        protected void writeJsonAttributes(JSONStringer out) throws JSONException {
            if (mNewParentId != null) {
                out.key("newParentId");
                out.value(mNewParentId);
            }
            if (mNewInputName != null) {
                out.key("newInputName");
                out.value(mNewInputName);
            }
            if (mHasNewPosition) {
                out.key("newCoordinate");
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
    public static class UIEvent extends BlocklyEvent {
        private final @BlocklyEvent.UIElement String mUiElement;
        private final String mOldValue;
        private final String mNewValue;

        UIEvent(String workspaceId, String groupId, @Nullable String blockId,
                @BlocklyEvent.UIElement String element, String oldValue, String newValue) {
            super(TYPE_UI, workspaceId, groupId, blockId);
            this.mUiElement = validateUiElement(element);
            this.mOldValue = oldValue;
            this.mNewValue = newValue;
        }

        UIEvent(String workspaceId, String groupId, @Nullable String blockId,
                @BlocklyEvent.UIElement String element, boolean oldValue, boolean newValue) {
            super(TYPE_UI, workspaceId, groupId, blockId);
            this.mUiElement = validateUiElement(element);
            this.mOldValue = oldValue ? "true" : "false";
            this.mNewValue = newValue ? "true" : "false";
        }

        public String getElement() {
            return mUiElement;
        }

        public String getOldElement() {
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
        }
    }
}
