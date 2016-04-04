package com.google.blockly.model;

import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Adds a toggleable checkbox to an Input.
 */
public final class FieldCheckbox extends Field<FieldCheckbox.Observer> {
    private boolean mChecked;

    public FieldCheckbox(String name, boolean checked) {
        super(name, TYPE_CHECKBOX);
        mChecked = checked;
    }

    public static FieldCheckbox fromJson(JSONObject json) {
        return new FieldCheckbox(
                json.optString("name", "NAME"),
                json.optBoolean("checked", true));
    }

    @Override
    public FieldCheckbox clone() {
        return new FieldCheckbox(getName(), mChecked);
    }

    @Override
    public boolean setFromString(String text) {
        mChecked = Boolean.parseBoolean(text);
        return true;
    }

    /**
     * @return The current state of the checkbox.
     */
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Sets the state of the checkbox.
     */
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            onCheckChanged(checked);
        }
    }

    @Override
    protected void serializeInner(XmlSerializer serializer) throws IOException {
        serializer.text(mChecked ? "true" : "false");
    }

    private void onCheckChanged(boolean newState) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onCheckChanged(this, newState);
        }
    }

    /**
     * Observer for listening to changes to a checkbox field.
     */
    public interface Observer {
        /**
         * Called when the field's checked value changed.
         *
         * @param field The field that changed.
         * @param newState The new state of the checkbox.
         */
        void onCheckChanged(FieldCheckbox field, boolean newState);
    }
}
