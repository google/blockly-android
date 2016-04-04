package com.google.blockly.model;

import android.text.TextUtils;

import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Adds a text to an Input. This can be used to add text to the block or label
 * another field. The text is not modifiable by the user.
 */
public final class FieldLabel extends Field<FieldLabel.Observer> {
    private String mText;

    public FieldLabel(String name, String text) {
        super(name, TYPE_LABEL);
        mText = text == null ? "" : text;
    }

    public static FieldLabel fromJson(JSONObject json) {
        return new FieldLabel(
                json.optString("name", null),
                json.optString("text", ""));
    }

    @Override
    public FieldLabel clone() {
        return new FieldLabel(getName(), mText);
    }

    /**
     * @return The text for this label.
     */
    public String getText() {
        return mText;
    }

    /**
     * Sets the text for this label. Changes to the label will not be serialized by Blockly and
     * should not be caused by user input. For user editable text fields use
     * {@link FieldInput} instead.
     */
    public void setText(String text) {
        if (!TextUtils.equals(text, mText)) {
            String oldText = mText;
            mText = text;
            onTextChanged(oldText, text);
        }
    }

    @Override
    public boolean setFromString(String text) {
        throw new IllegalStateException("Label field text cannot be set after construction.");
    }

    @Override
    protected void serializeInner(XmlSerializer serializer) throws IOException {
        // Nothing to do.
    }

    private void onTextChanged(String oldText, String newText) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onTextChanged(this, oldText, newText);
        }
    }

    /**
     * Observer for listening to changes to an input field.
     */
    public interface Observer {
        /**
         * Called when the field's text changed.
         * <p>
         * Note: Label fields are not expected to be user editable and are not serialized by
         * Blockly's core library.
         *
         * @param field The field that changed.
         * @param oldText The field's previous text.
         * @param newText The field's new text.
         */
        void onTextChanged(FieldLabel field, String oldText, String newText);
    }
}
