/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.model;

import android.database.Observable;
import android.support.annotation.IntDef;

import com.google.blockly.model.BlocklyEvent.ChangeEvent;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The base class for Fields in Blockly. A field is the smallest piece of a {@link Block} and is
 * wrapped by an {@link Input}.
 */
public abstract class Field extends Observable<Field.Observer> implements Cloneable {
    private static final String TAG = "Field";

    // TODO: These FieldTypes are not extensible without editing this file. It should be possible
    //       to add a new field type by extending BlockFactory and BlockViewFactory.
    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_LABEL = 0;
    public static final int TYPE_INPUT = 1;
    public static final int TYPE_ANGLE = 2;
    public static final int TYPE_CHECKBOX = 3;
    public static final int TYPE_COLOR = 4;
    public static final int TYPE_DATE = 5;
    public static final int TYPE_VARIABLE = 6;
    public static final int TYPE_DROPDOWN = 7;
    public static final int TYPE_IMAGE = 8;
    public static final int TYPE_NUMBER = 9;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_UNKNOWN, TYPE_LABEL, TYPE_INPUT, TYPE_ANGLE, TYPE_CHECKBOX, TYPE_COLOR,
            TYPE_DATE, TYPE_VARIABLE, TYPE_DROPDOWN, TYPE_IMAGE, TYPE_NUMBER})
    public @interface FieldType {}
    // When adding fields, also update stringToFieldType() below.

    public static final String TYPE_LABEL_STRING = "field_label";
    public static final String TYPE_INPUT_STRING = "field_input";
    public static final String TYPE_ANGLE_STRING = "field_angle";
    public static final String TYPE_CHECKBOX_STRING = "field_checkbox";
    public static final String TYPE_COLOR_STRING = "field_colour";
    public static final String TYPE_DATE_STRING = "field_date";
    public static final String TYPE_VARIABLE_STRING = "field_variable";
    public static final String TYPE_DROPDOWN_STRING = "field_dropdown";
    public static final String TYPE_IMAGE_STRING = "field_image";
    public static final String TYPE_NUMBER_STRING = "field_number";
    // Icon fields are not explicitly declared, so they have no string representation

    /**
     * Observer for listening to changes to a field.
     */
    public interface Observer {
        /**
         * Called when the field's text changed.
         *
         * @param field The field that changed.
         * @param oldValue The field's previous value, in serialized string form.
         * @param newValue The field's new value, in serialized string form.
         */
        void onValueChanged(Field field, String oldValue, String newValue);
    }

    private final String mName;
    private final int mType;
    private Block mBlock;

    public Field(String name, @FieldType int type) {
        mName = name;
        mType = type;
    }

    @Override
    public Field clone() throws CloneNotSupportedException {
        return (Field) super.clone();
    }

    /**
     * Writes information about the editable parts of the field as XML.
     *
     * @param serializer The XmlSerializer to write to.
     *
     * @throws IOException
     */
    public void serialize(XmlSerializer serializer) throws IOException {
        if (mType == TYPE_LABEL || mType == TYPE_IMAGE) {
            return;
        }
        serializer.startTag(null, "field").attribute(null, "name", mName);
        serializer.text(getSerializedValue());
        serializer.endTag(null, "field");
    }

    /**
     * Get the name of this field. Names, if they are not null, are expected to be unique within a
     * block but are not guaranteed to be.
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the type of this field. There should be a concrete implementation for each field type.
     */
    @FieldType
    public int getType() {
        return mType;
    }

    /**
     * @return The parent block for this field.
     */
    public Block getBlock() {
        return mBlock;
    }

    /**
     * Sets the values of the field from a string.
     * <p/>
     * This is used for setting values of all types of fields when loading a workspace from XML. It
     * is also used, however, as the primary means of setting text fields (e.g., inputs, labels,
     * dates).
     * <p/>
     * There should be a concrete implementation for each field type.
     *
     * @param text The text value for this field from the XML.
     *
     * @return True if the value was set, false otherwise.
     */
    public abstract boolean setFromString(String text);

    /**
     * @return The value serialized into a string.
     */
    public abstract String getSerializedValue();

    /**
     * Sets the parent block for this field. Should only be used during block intialization.
     *
     * @param block The parent block for this field.
     */
    protected void setBlock(Block block) {
        if (block == mBlock) {
            return;
        }
        if (block != null && mBlock != null) {
            throw new IllegalStateException("Field is already a member of another block.");
        }
        mBlock = block;
    }

    /**
     * Checks if the given type name is a known field type.
     *
     * @param typeString The type to check.
     *
     * @return true if this is a known field type, false otherwise.
     */
    public static boolean isFieldType(String typeString) {
        return stringToFieldType(typeString) != TYPE_UNKNOWN;
    }

    /**
     * Convert string representation of field type to internal integer Id.
     *
     * @param typeString The field type string, e.g., TYPE_LABEL_STRING ("field_label").
     *
     * @return The integer Id representing the given field type string, or {@code #TYPE_UNKNOWN} if
     * the string does not represent a valid type.
     *
     * @throws IllegalArgumentException If the string is null.
     */
    @FieldType
    private static int stringToFieldType(String typeString) {
        if (typeString == null) {
            throw new IllegalArgumentException("type may not be null");
        }

        switch (typeString) {
            case TYPE_LABEL_STRING:
                return TYPE_LABEL;
            case TYPE_INPUT_STRING:
                return TYPE_INPUT;
            case TYPE_ANGLE_STRING:
                return TYPE_ANGLE;
            case TYPE_CHECKBOX_STRING:
                return TYPE_CHECKBOX;
            case TYPE_COLOR_STRING:
                return TYPE_COLOR;
            case TYPE_DATE_STRING:
                return TYPE_DATE;
            case TYPE_VARIABLE_STRING:
                return TYPE_VARIABLE;
            case TYPE_DROPDOWN_STRING:
                return TYPE_DROPDOWN;
            case TYPE_IMAGE_STRING:
                return TYPE_IMAGE;
            case TYPE_NUMBER_STRING:
                return TYPE_NUMBER;
            default:
                return TYPE_UNKNOWN;
        }
    }

    /**
     * Triggers events (both {@link ChangeEvent}s and {@link Observer}s) in response to a value
     * change. If the field is attached to a event workspace (workspace, toolbox, or trash), must be
     * called on the main thread.
     *
     * @param oldValueString Old value in serialized string form.
     * @param newValueString New value in serialized string form.
     */
    protected void fireValueChanged(final String oldValueString, final String newValueString) {
        runAsPossibleEventGroup(new Runnable() {
            @Override
            public void run() {
                if (mBlock != null) {
                    mBlock.maybeAddPendingChangeEvent(
                            BlocklyEvent.ELEMENT_FIELD, Field.this, oldValueString, newValueString);
                }
                for (int i = 0; i < mObservers.size(); i++) {
                    mObservers.get(i).onValueChanged(Field.this, oldValueString, newValueString);
                }
            }
        });
    }

    /**
     * Runs the runnable immediately, as an event group if connected to a block & controller.
     * @param runnable
     */
    private void runAsPossibleEventGroup(Runnable runnable) {
        if (mBlock != null) {
            mBlock.runAsPossibleEventGroup(runnable);
        } else {
            runnable.run();
        }
    }
}
